package dev.repost.admin.auth

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import java.time.Duration
import java.time.OffsetDateTime

fun Route.configureAuthRoutes(authService: AuthService) {
    route("/api/v1/admin/auth") {
        post("/login", {
            tags = listOf("admin")
            summary = "Admin login"
            description = "Authenticates with email/password. Returns access JWT; sets refresh cookie."
            request {
                body<LoginRequest>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Authenticated"
                    body<TokenResponse>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Invalid credentials"
                }
            }
        }) {
            val body = call.receive<LoginRequest>()
            val session =
                authService.login(body.email, body.password)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_credentials"))

            call.setRefreshCookie(session.refreshToken, session.refreshExpiresAt)
            call.respond(
                TokenResponse(
                    accessToken = session.accessToken,
                    expiresIn = session.expiresIn,
                ),
            )
        }

        post("/refresh", {
            tags = listOf("admin")
            summary = "Refresh access token"
            response {
                HttpStatusCode.OK to {
                    description = "New access token"
                    body<TokenResponse>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Missing or invalid refresh cookie"
                }
            }
        }) {
            val raw = call.request.cookies[AuthService.REFRESH_COOKIE]
            val session =
                raw?.let { authService.refresh(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_refresh"))

            call.setRefreshCookie(session.refreshToken, session.refreshExpiresAt)
            call.respond(
                TokenResponse(
                    accessToken = session.accessToken,
                    expiresIn = session.expiresIn,
                ),
            )
        }

        post("/logout", {
            tags = listOf("admin")
            summary = "Admin logout"
            response {
                HttpStatusCode.NoContent to {
                    description = "Logged out"
                }
            }
        }) {
            authService.logout(call.request.cookies[AuthService.REFRESH_COOKIE])
            call.clearRefreshCookie()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.setRefreshCookie(
    token: String,
    expiresAt: OffsetDateTime,
) {
    val maxAge = Duration.between(OffsetDateTime.now(), expiresAt).seconds.coerceAtLeast(0)
    response.cookies.append(
        name = AuthService.REFRESH_COOKIE,
        value = token,
        maxAge = maxAge,
        path = "/api/v1/admin/auth",
        httpOnly = true,
        secure = isSecureCookies(),
        extensions = mapOf("SameSite" to "Strict"),
    )
}

private fun io.ktor.server.application.ApplicationCall.clearRefreshCookie() {
    response.cookies.append(
        name = AuthService.REFRESH_COOKIE,
        value = "",
        maxAge = 0,
        path = "/api/v1/admin/auth",
        httpOnly = true,
        secure = isSecureCookies(),
        extensions = mapOf("SameSite" to "Strict"),
    )
}

private fun isSecureCookies(): Boolean =
    System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull()
        ?: (System.getenv("API_BASE_URL")?.startsWith("https://") == true)
