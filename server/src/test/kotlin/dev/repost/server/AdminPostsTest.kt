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
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
class AdminPostsTest {
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
    fun `posts CRUD requires bearer and persists content`() =
        testApplication {
            application { module(authService) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/admin/posts").status)

            val token = login(client)

            val created =
                client.post("/api/v1/admin/posts") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertPostRequest(
                            slug = "hello-world",
                            title = "Hello World",
                            excerpt = "First post",
                            contentMd = "# Hello\n\nSome words here for reading time.",
                            status = "published",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, created.status)
            val createdBody = Json.parseToJsonElement(created.bodyAsText()).jsonObject
            val id = createdBody["id"]!!.jsonPrimitive.content
            assertEquals("hello-world", createdBody["slug"]!!.jsonPrimitive.content)
            assertEquals("published", createdBody["status"]!!.jsonPrimitive.content)

            val listed =
                client.get("/api/v1/admin/posts") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.OK, listed.status)
            val items = Json.parseToJsonElement(listed.bodyAsText()).jsonObject["items"]!!.jsonArray
            assertTrue(items.any { it.jsonObject["id"]!!.jsonPrimitive.content == id })

            val updated =
                client.put("/api/v1/admin/posts/$id") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertPostRequest(
                            slug = "hello-world",
                            title = "Hello World Updated",
                            contentMd = "Updated body",
                            status = "draft",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, updated.status)
            assertEquals(
                "Hello World Updated",
                Json.parseToJsonElement(updated.bodyAsText()).jsonObject["title"]!!.jsonPrimitive.content,
            )

            val deleted =
                client.delete("/api/v1/admin/posts/$id") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.NoContent, deleted.status)

            val missing =
                client.get("/api/v1/admin/posts/$id") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.NotFound, missing.status)
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
