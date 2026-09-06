package dev.repost.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorsTest {
    @Test
    fun `preflight from manager origin allows credentials`() =
        testApplication {
            application { module() }

            val response =
                client.options("/api/v1/admin/auth/login") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Post.value)
                    header(HttpHeaders.AccessControlRequestHeaders, HttpHeaders.ContentType)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("true", response.headers[HttpHeaders.AccessControlAllowCredentials])
            assertEquals("http://localhost:5173", response.headers[HttpHeaders.AccessControlAllowOrigin])
            assertTrue(
                response.headers[HttpHeaders.AccessControlAllowHeaders]
                    ?.contains(HttpHeaders.Authorization, ignoreCase = true) == true ||
                    response.headers[HttpHeaders.AccessControlAllowHeaders]
                        ?.contains(HttpHeaders.ContentType, ignoreCase = true) == true,
            )
        }

    @Test
    fun `simple get reflects manager origin with credentials`() =
        testApplication {
            application { module() }

            val response =
                client.get("/health") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("true", response.headers[HttpHeaders.AccessControlAllowCredentials])
            assertEquals("http://localhost:5173", response.headers[HttpHeaders.AccessControlAllowOrigin])
        }
}
