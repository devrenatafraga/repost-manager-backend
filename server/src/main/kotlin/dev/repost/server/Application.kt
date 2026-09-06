package dev.repost.server

import dev.repost.admin.configureAdminRoutes
import dev.repost.admin.auth.AuthService
import dev.repost.admin.auth.JwtConfig
import dev.repost.admin.auth.JwtService
import dev.repost.db.AdminSeeder
import dev.repost.db.DatabaseConfig
import dev.repost.db.DatabaseFactory
import dev.repost.db.DatabaseMigrator
import dev.repost.publicapi.configurePublicRoutes
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.ktor.http.HttpStatusCode
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
import kotlinx.serialization.json.Json

fun main() {
    val dbConfig = DatabaseConfig.fromEnvironment()
    if (dbConfig != null) {
        val applied =
            DatabaseMigrator.migrate(
                jdbcUrl = dbConfig.jdbcUrl,
                user = dbConfig.user,
                password = dbConfig.password,
            )
        println("Flyway applied $applied migration(s)")
        DatabaseFactory.connect(dbConfig)
        AdminSeeder.seedFromEnvironment()
    }

    val authService = buildAuthService()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        module(authService)
    }.start(wait = true)
}

fun buildAuthService(): AuthService? {
    val jwtConfig = JwtConfig.fromEnvironment() ?: return null
    if (DatabaseConfig.fromEnvironment() == null) return null
    return AuthService(JwtService(jwtConfig))
}

fun Application.module(authService: AuthService? = null) {
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
        configureAdminRoutes(authService)
    }
}
