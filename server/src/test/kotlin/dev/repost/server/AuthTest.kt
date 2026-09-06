package dev.repost.server

import dev.repost.admin.auth.AuthService
import dev.repost.admin.auth.JwtConfig
import dev.repost.admin.auth.JwtService
import dev.repost.admin.auth.LoginRequest
import dev.repost.db.DatabaseConfig
import dev.repost.db.DatabaseFactory
import dev.repost.db.DatabaseMigrator
import dev.repost.db.PasswordHasher
import dev.repost.db.UserRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthTest {
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
                    JwtConfig(
                        secret = "test-secret-at-least-32-characters-long",
                    ),
                ),
            )
    }

    @Test
    fun `login with valid credentials returns access token and sets refresh cookie`() =
        testApplication {
            application { module(authService) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            val response =
                client.post("/api/v1/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, password))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertNotNull(body["accessToken"]?.jsonPrimitive?.content)
            assertNotNull(extractRefreshCookie(response))
        }

    @Test
    fun `login with invalid password returns 401`() =
        testApplication {
            application { module(authService) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            val response =
                client.post("/api/v1/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, "wrong-password"))
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `refresh rotates access token using cookie`() =
        testApplication {
            application { module(authService) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            val login =
                client.post("/api/v1/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, password))
                }
            val refreshCookie = extractRefreshCookie(login)
            assertNotNull(refreshCookie)

            val refreshed =
                client.post("/api/v1/admin/auth/refresh") {
                    header(HttpHeaders.Cookie, refreshCookie)
                }
            assertEquals(HttpStatusCode.OK, refreshed.status)
            val body = Json.parseToJsonElement(refreshed.bodyAsText()).jsonObject
            assertNotNull(body["accessToken"]?.jsonPrimitive?.content)
        }

    private fun extractRefreshCookie(response: HttpResponse): String? {
        val setCookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        val match =
            setCookies
                .asSequence()
                .mapNotNull { cookie ->
                    Regex("""${AuthService.REFRESH_COOKIE}=([^;]+)""")
                        .find(cookie)
                        ?.groupValues
                        ?.get(1)
                }.firstOrNull()
        return match?.let { "${AuthService.REFRESH_COOKIE}=$it" }
    }
}
