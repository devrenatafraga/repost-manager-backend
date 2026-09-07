package dev.repost.server

import dev.repost.admin.auth.AuthService
import dev.repost.admin.auth.JwtConfig
import dev.repost.admin.auth.JwtService
import dev.repost.admin.auth.LoginRequest
import dev.repost.admin.posts.UpsertPostRequest
import dev.repost.db.DatabaseConfig
import dev.repost.db.DatabaseFactory
import dev.repost.db.DatabaseMigrator
import dev.repost.db.PasswordHasher
import dev.repost.db.UserRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicPostsTest {
    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("repost")
                .withUsername("repost")
                .withPassword("repost")
    }

    private lateinit var authService: AuthService
    private val password = "test-password-123"
    private val email = "admin@repost.test"

    @BeforeAll
    fun setup() {
        DatabaseMigrator.migrate(postgres.jdbcUrl, postgres.username, postgres.password)
        DatabaseFactory.connect(
            DatabaseConfig(
                jdbcUrl = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
            ),
        )
        UserRepository.upsertAdmin(email, PasswordHasher.hash(password))
        authService =
            AuthService(
                JwtService(
                    JwtConfig(secret = "test-secret-at-least-32-characters-long"),
                ),
            )
    }

    @Test
    fun `public list and get expose only published posts`() =
        testApplication {
            application { module(authService) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            val token = login(client)

            client.post("/api/v1/admin/posts") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    UpsertPostRequest(
                        slug = "draft-only",
                        title = "Draft",
                        contentMd = "secret",
                        status = "draft",
                    ),
                )
            }.also { assertEquals(HttpStatusCode.Created, it.status) }

            client.post("/api/v1/admin/posts") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    UpsertPostRequest(
                        slug = "hello-public",
                        title = "Hello Public",
                        excerpt = "Visible",
                        contentMd = "Public body",
                        status = "published",
                    ),
                )
            }.also { assertEquals(HttpStatusCode.Created, it.status) }

            val list = client.get("/api/v1/public/posts")
            assertEquals(HttpStatusCode.OK, list.status)
            val items = Json.parseToJsonElement(list.bodyAsText()).jsonObject["items"]!!.jsonArray
            assertEquals(1, items.size)
            assertEquals("hello-public", items[0].jsonObject["slug"]!!.jsonPrimitive.content)

            val bySlug = client.get("/api/v1/public/posts/hello-public")
            assertEquals(HttpStatusCode.OK, bySlug.status)
            assertEquals(
                "Hello Public",
                Json.parseToJsonElement(bySlug.bodyAsText()).jsonObject["title"]!!.jsonPrimitive.content,
            )

            val draftHidden = client.get("/api/v1/public/posts/draft-only")
            assertEquals(HttpStatusCode.NotFound, draftHidden.status)

            val missing = client.get("/api/v1/public/posts/nope")
            assertEquals(HttpStatusCode.NotFound, missing.status)

            assertTrue(list.headers[HttpHeaders.Authorization] == null)
        }

    private suspend fun login(client: io.ktor.client.HttpClient): String {
        val login =
            client.post("/api/v1/admin/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
        assertEquals(HttpStatusCode.OK, login.status)
        return Json
            .parseToJsonElement(login.bodyAsText())
            .jsonObject["accessToken"]!!
            .jsonPrimitive
            .content
    }
}
