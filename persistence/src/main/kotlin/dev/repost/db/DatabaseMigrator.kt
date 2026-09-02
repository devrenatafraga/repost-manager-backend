package dev.repost.db

import org.flywaydb.core.Flyway

object DatabaseMigrator {
    fun migrate(
        jdbcUrl: String,
        user: String? = null,
        password: String? = null,
    ): Int {
        val config =
            Flyway
                .configure()
                .locations("classpath:db/migration")

        val flyway =
            if (!user.isNullOrBlank() && password != null) {
                config.dataSource(jdbcUrl, user, password)
            } else {
                config.dataSource(jdbcUrl)
            }.load()

        return flyway.migrate().migrationsExecuted
    }
}
