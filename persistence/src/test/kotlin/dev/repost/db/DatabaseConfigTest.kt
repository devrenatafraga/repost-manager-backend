package dev.repost.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DatabaseConfigTest {
    @Test
    fun `toJdbcUrl accepts jdbc URLs unchanged`() {
        val url = "jdbc:postgresql://localhost:5432/repost"
        assertEquals(url, DatabaseConfig.toJdbcUrl(url))
    }

    @Test
    fun `toJdbcUrl normalizes postgresql scheme`() {
        assertEquals(
            "jdbc:postgresql://localhost:5432/repost",
            DatabaseConfig.toJdbcUrl("postgresql://localhost:5432/repost"),
        )
    }

    @Test
    fun `toJdbcUrl normalizes postgres scheme`() {
        assertEquals(
            "jdbc:postgresql://localhost:5432/repost",
            DatabaseConfig.toJdbcUrl("postgres://localhost:5432/repost"),
        )
    }

    @Test
    fun `toJdbcUrl rejects unsupported schemes`() {
        assertThrows<IllegalStateException> {
            DatabaseConfig.toJdbcUrl("mysql://localhost:3306/repost")
        }
    }
}
