package dev.repost.db

import dev.repost.db.tables.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

object UserRepository {
    fun findByEmail(email: String): UserRecord? =
        transaction {
            Users
                .selectAll()
                .where { Users.email eq email.lowercase() }
                .map { it.toUserRecord() }
                .singleOrNull()
        }

    fun findById(id: UUID): UserRecord? =
        transaction {
            Users
                .selectAll()
                .where { Users.id eq id }
                .map { it.toUserRecord() }
                .singleOrNull()
        }

    fun upsertAdmin(
        email: String,
        passwordHash: String,
    ): UserRecord =
        transaction {
            val normalized = email.lowercase()
            val existing = Users.selectAll().where { Users.email eq normalized }.singleOrNull()
            if (existing != null) {
                Users.update({ Users.id eq existing[Users.id] }) {
                    it[Users.passwordHash] = passwordHash
                    it[Users.role] = "admin"
                }
                findById(existing[Users.id])!!
            } else {
                val id = UUID.randomUUID()
                Users.insert {
                    it[Users.id] = id
                    it[Users.email] = normalized
                    it[Users.passwordHash] = passwordHash
                    it[Users.role] = "admin"
                }
                findById(id)!!
            }
        }

    private fun ResultRow.toUserRecord() =
        UserRecord(
            id = this[Users.id],
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            role = this[Users.role],
        )
}
