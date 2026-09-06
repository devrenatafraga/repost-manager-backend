package dev.repost.server

import dev.repost.admin.auth.AuthRateLimiter
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
class AdminJwtGuardTest {
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
    fun `admin surface without bearer returns 401`() =
        testApplication {
            application { module(authService) }

            val response = client.get("/api/v1/admin/")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `admin surface with valid bearer returns 200`() =
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
            assertEquals(HttpStatusCode.OK, login.status)
            val token =
                Json
                    .parseToJsonElement(login.bodyAsText())
                    .jsonObject["accessToken"]!!
                    .jsonPrimitive
                    .content

            val response =
                client.get("/api/v1/admin/") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("admin"))
        }

    @Test
    fun `login is rate limited after repeated attempts`() =
        testApplication {
            val limiter = AuthRateLimiter(maxAttempts = 3, windowMs = 60_000)
            application { module(authService, rateLimiter = limiter) }
            val client =
                createClient {
                    install(ClientContentNegotiation) { json() }
                }

            repeat(3) {
                val response =
                    client.post("/api/v1/admin/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest(email, "wrong-password"))
                    }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }

            val blocked =
                client.post("/api/v1/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, "wrong-password"))
                }
            assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        }
}
