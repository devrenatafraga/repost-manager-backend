package dev.repost.server

import dev.repost.admin.configureAdminRoutes
import dev.repost.publicapi.configurePublicRoutes
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    install(OpenApi) {
        info {
            title = "Repost Manager API"
            version = "1.0.0"
            description = "Contract for admin and public surfaces (ADR-0003)."
        }
        server {
            url = System.getenv("API_BASE_URL") ?: "http://localhost:8080"
        }
    }

    routing {
        route("/openapi.json") {
            openApi()
        }

        get("/health", {
            tags = listOf("system")
            summary = "Health check"
            response {
                HttpStatusCode.OK to {
                    description = "Service is up"
                    body<Map<String, String>> {
                        example("default") {
                            value = mapOf("status" to "ok")
                        }
                    }
                }
            }
        }) {
            call.respond(mapOf("status" to "ok"))
        }

        configurePublicRoutes()
        configureAdminRoutes()
    }
}
