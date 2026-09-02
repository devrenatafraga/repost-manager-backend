package dev.repost.db

data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String?,
    val password: String?,
) {
    companion object {
        fun fromEnvironment(): DatabaseConfig? {
            val rawUrl = System.getenv("DATABASE_URL") ?: return null
            val jdbcUrl = toJdbcUrl(rawUrl)
            return DatabaseConfig(
                jdbcUrl = jdbcUrl,
                user = System.getenv("DATABASE_USER")?.takeIf { it.isNotBlank() },
                password = System.getenv("DATABASE_PASSWORD"),
            )
        }

        fun toJdbcUrl(rawUrl: String): String =
            when {
                rawUrl.startsWith("jdbc:") -> rawUrl
                rawUrl.startsWith("postgresql://") -> "jdbc:$rawUrl"
                else -> error("Unsupported DATABASE_URL format: expected jdbc: or postgresql://")
            }
    }
}
