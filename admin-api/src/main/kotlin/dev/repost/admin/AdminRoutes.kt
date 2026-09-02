package dev.repost.admin

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.configureAdminRoutes() {
    route("/api/v1/admin") {
        get("/", {
            tags = listOf("admin")
            summary = "Admin API surface"
            description = "Stub endpoint for the authenticated admin surface."
            response {
                HttpStatusCode.OK to {
                    description = "Surface metadata"
                    body<Map<String, String>> {
                        example("default") {
                            value = mapOf("surface" to "admin", "version" to "v1")
                        }
                    }
                }
            }
        }) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "surface" to "admin",
                    "version" to "v1",
                ),
            )
        }
    }
}
