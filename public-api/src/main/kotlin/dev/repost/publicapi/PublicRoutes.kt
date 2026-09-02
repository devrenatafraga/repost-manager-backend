package dev.repost.publicapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.configurePublicRoutes() {
    route("/api/v1/public") {
        get("/") {
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
