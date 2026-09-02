package dev.repost.db

fun main() {
    val config =
        DatabaseConfig.fromEnvironment()
            ?: error("Set DATABASE_URL, DATABASE_USER and DATABASE_PASSWORD (see .env.example)")

    val applied = DatabaseMigrator.migrate(config.jdbcUrl, config.user, config.password)
    println("Flyway applied $applied migration(s) to ${config.jdbcUrl}")
}
