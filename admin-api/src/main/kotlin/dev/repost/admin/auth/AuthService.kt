package dev.repost.admin.auth

import dev.repost.db.PasswordHasher
import dev.repost.db.RefreshTokenRepository
import dev.repost.db.UserRecord
import dev.repost.db.UserRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.HexFormat

class AuthService(
    private val jwtService: JwtService,
    private val refreshTtlDays: Long = 14,
) {
    data class Session(
        val accessToken: String,
        val expiresIn: Long,
        val refreshToken: String,
        val refreshExpiresAt: OffsetDateTime,
        val user: UserRecord,
    )

    fun login(
        email: String,
        password: String,
    ): Session? {
        val user = UserRepository.findByEmail(email) ?: return null
        if (!PasswordHasher.matches(password, user.passwordHash)) return null
        return issueSession(user)
    }

    fun refresh(rawRefreshToken: String): Session? {
        val hash = hashToken(rawRefreshToken)
        val stored = RefreshTokenRepository.findValidByHash(hash) ?: return null
        val user = UserRepository.findById(stored.userId) ?: return null
        RefreshTokenRepository.revokeByHash(hash)
        return issueSession(user)
    }

    fun logout(rawRefreshToken: String?) {
        if (rawRefreshToken.isNullOrBlank()) return
        RefreshTokenRepository.revokeByHash(hashToken(rawRefreshToken))
    }

    private fun issueSession(user: UserRecord): Session {
        val (accessToken, expiresIn) = jwtService.issueAccessToken(user.id, user.email, user.role)
        val rawRefresh = generateRefreshToken()
        val refreshExpiresAt = OffsetDateTime.now().plusDays(refreshTtlDays)
        RefreshTokenRepository.insert(
            userId = user.id,
            tokenHash = hashToken(rawRefresh),
            expiresAt = refreshExpiresAt,
        )
        return Session(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshToken = rawRefresh,
            refreshExpiresAt = refreshExpiresAt,
            user = user,
        )
    }

    companion object {
        const val REFRESH_COOKIE = "repost_refresh"

        fun hashToken(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return HexFormat.of().formatHex(digest)
        }

        private fun generateRefreshToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
