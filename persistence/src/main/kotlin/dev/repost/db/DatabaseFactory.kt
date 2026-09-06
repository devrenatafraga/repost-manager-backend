package dev.repost.db

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    @Volatile
    private var connected = false

    fun connect(config: DatabaseConfig) {
        Database.connect(
            url = config.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = config.user,
            password = config.password,
        )
        connected = true
    }

    fun isConnected(): Boolean = connected
}
