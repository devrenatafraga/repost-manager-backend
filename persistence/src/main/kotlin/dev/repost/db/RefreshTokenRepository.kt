package dev.repost.db

import dev.repost.db.tables.RefreshTokens
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.UUID

data class RefreshTokenRecord(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)

object RefreshTokenRepository {
    fun insert(
        userId: UUID,
        tokenHash: String,
        expiresAt: OffsetDateTime,
    ): UUID =
        transaction {
            val id = UUID.randomUUID()
            RefreshTokens.insert {
                it[RefreshTokens.id] = id
                it[RefreshTokens.userId] = userId
                it[RefreshTokens.tokenHash] = tokenHash
                it[RefreshTokens.expiresAt] = expiresAt
            }
            id
        }

    fun findValidByHash(tokenHash: String): RefreshTokenRecord? =
        transaction {
            RefreshTokens
                .selectAll()
                .where { RefreshTokens.tokenHash eq tokenHash }
                .map {
                    RefreshTokenRecord(
                        id = it[RefreshTokens.id],
                        userId = it[RefreshTokens.userId],
                        tokenHash = it[RefreshTokens.tokenHash],
                        expiresAt = it[RefreshTokens.expiresAt],
                        revokedAt = it[RefreshTokens.revokedAt],
                    )
                }.singleOrNull()
                ?.takeIf { it.revokedAt == null && it.expiresAt.isAfter(OffsetDateTime.now()) }
        }

    fun revokeByHash(tokenHash: String) {
        transaction {
            RefreshTokens.update({ RefreshTokens.tokenHash eq tokenHash }) {
                it[revokedAt] = OffsetDateTime.now()
            }
        }
    }

    fun revokeAllForUser(userId: UUID) {
        transaction {
            RefreshTokens.update({
                (RefreshTokens.userId eq userId) and RefreshTokens.revokedAt.isNull()
            }) {
                it[revokedAt] = OffsetDateTime.now()
            }
        }
    }
}
