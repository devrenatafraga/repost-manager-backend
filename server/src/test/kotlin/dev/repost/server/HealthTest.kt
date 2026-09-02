package dev.repost.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthTest {
    @Test
    fun `health returns 200`() = testApplication {
        application { module() }

        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("ok"))
        }
    }

    @Test
    fun `public surface is reachable`() = testApplication {
        application { module() }

        client.get("/api/v1/public/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("public"))
        }
    }

    @Test
    fun `admin surface is reachable`() = testApplication {
        application { module() }

        client.get("/api/v1/admin/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("admin"))
        }
    }
}
