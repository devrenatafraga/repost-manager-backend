package dev.repost.admin

import dev.repost.admin.auth.ADMIN_JWT_AUTH
import dev.repost.admin.auth.AuthRateLimiter
import dev.repost.admin.auth.AuthService
import dev.repost.admin.auth.configureAuthRoutes
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.configureAdminRoutes(
    authService: AuthService? = null,
    rateLimiter: AuthRateLimiter = AuthRateLimiter(),
    protectWithJwt: Boolean = authService != null,
) {
    authService?.let { configureAuthRoutes(it, rateLimiter) }

    if (protectWithJwt) {
        authenticate(ADMIN_JWT_AUTH) {
            adminSurfaceRoutes()
        }
    } else {
        adminSurfaceRoutes()
    }
}

private fun Route.adminSurfaceRoutes() {
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
                HttpStatusCode.Unauthorized to {
                    description = "Missing or invalid Bearer JWT"
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
