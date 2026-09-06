package dev.repost.admin.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

const val ADMIN_JWT_AUTH = "admin-jwt"

fun Application.installAdminJwtAuth(jwtService: JwtService) {
    install(Authentication) {
        jwt(ADMIN_JWT_AUTH) {
            verifier(jwtService.verifier())
            validate { credential ->
                val role = credential.payload.getClaim("role").asString()
                if (role == "admin" && credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
            }
        }
    }
}
