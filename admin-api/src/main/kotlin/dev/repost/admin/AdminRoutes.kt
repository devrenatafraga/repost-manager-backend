package dev.repost.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.configureAdminRoutes() {
    route("/api/v1/admin") {
        get("/") {
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
