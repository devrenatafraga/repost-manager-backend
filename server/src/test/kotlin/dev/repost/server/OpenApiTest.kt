package dev.repost.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiTest {
    @Test
    fun `openapi json is served with admin and public tags`() = testApplication {
        application { module() }

        client.get("/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertTrue(body.contains("\"openapi\""), "must be OpenAPI document")
            assertTrue(body.contains("\"admin\""), "must include admin tag")
            assertTrue(body.contains("\"public\""), "must include public tag")
            assertTrue(body.contains("/api/v1/admin"), "must document admin path")
            assertTrue(body.contains("/api/v1/public"), "must document public path")
        }
    }
}
