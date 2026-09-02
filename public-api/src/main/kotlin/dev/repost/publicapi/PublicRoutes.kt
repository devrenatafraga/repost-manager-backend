package dev.repost.publicapi

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.configurePublicRoutes() {
    route("/api/v1/public") {
        get("/", {
            tags = listOf("public")
            summary = "Public API surface"
            description = "Stub endpoint for the anonymous, cacheable public surface."
            response {
                HttpStatusCode.OK to {
                    description = "Surface metadata"
                    body<Map<String, String>> {
                        example("default") {
                            value = mapOf("surface" to "public", "version" to "v1")
                        }
                    }
                }
            }
        }) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "surface" to "public",
                    "version" to "v1",
                ),
            )
        }
    }
}
