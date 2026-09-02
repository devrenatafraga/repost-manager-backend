package dev.repost.db

data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun fromEnvironment(): DatabaseConfig? {
            val rawUrl = System.getenv("DATABASE_URL") ?: return null
            val user = System.getenv("DATABASE_USER") ?: return null
            val password = System.getenv("DATABASE_PASSWORD") ?: return null
            return DatabaseConfig(
                jdbcUrl = toJdbcUrl(rawUrl),
                user = user,
                password = password,
            )
        }

        fun toJdbcUrl(rawUrl: String): String =
            when {
                rawUrl.startsWith("jdbc:") -> rawUrl
                rawUrl.startsWith("postgresql://") -> "jdbc:$rawUrl"
                rawUrl.startsWith("postgres://") ->
                    "jdbc:postgresql://${rawUrl.removePrefix("postgres://")}"
                else -> error("Unsupported DATABASE_URL format: expected jdbc:, postgresql:// or postgres://")
            }
    }
}
