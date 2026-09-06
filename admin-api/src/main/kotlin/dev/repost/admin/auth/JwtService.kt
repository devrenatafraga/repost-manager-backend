package dev.repost.admin.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.util.Date
import java.util.UUID

data class JwtConfig(
    val secret: String,
    val issuer: String = "repost",
    val audience: String = "repost-admin",
    val accessTtlSeconds: Long = 900,
) {
    companion object {
        fun fromEnvironment(): JwtConfig? {
            val secret = System.getenv("JWT_SECRET")?.takeIf { it.length >= 32 } ?: return null
            return JwtConfig(secret = secret)
        }
    }
}

class JwtService(
    private val config: JwtConfig,
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun issueAccessToken(
        userId: UUID,
        email: String,
        role: String,
    ): Pair<String, Long> {
        val expiresIn = config.accessTtlSeconds
        val token =
            JWT
                .create()
                .withIssuer(config.issuer)
                .withAudience(config.audience)
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("role", role)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(expiresIn)))
                .sign(algorithm)
        return token to expiresIn
    }

    fun verifier() =
        JWT
            .require(algorithm)
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()
}
