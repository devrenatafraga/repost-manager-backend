package dev.repost.db

import org.flywaydb.core.Flyway

object DatabaseMigrator {
    fun migrate(
        jdbcUrl: String,
        user: String,
        password: String,
    ): Int =
        Flyway
            .configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
            .migrationsExecuted
}
