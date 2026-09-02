package dev.repost.db

import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
class FlywayMigrationTest {
    companion object {
        @Container
        @JvmStatic
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("repost")
                .withUsername("repost")
                .withPassword("repost")
    }

    @Test
    fun `migrations apply schema seed and readonly role`() {
        val migrationsRun =
            DatabaseMigrator.migrate(
                jdbcUrl = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
            )

        assertTrue(migrationsRun >= 3, "expected at least V1, V2 and V3")

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().use { stmt ->
                val tables =
                    stmt.executeQuery(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_type = 'BASE TABLE'
                        ORDER BY table_name
                        """.trimIndent(),
                    ).use { rs ->
                        generateSequence { if (rs.next()) rs.getString("table_name") else null }.toList()
                    }

                assertTrue("users" in tables)
                assertTrue("blogs" in tables)
                assertTrue("posts" in tables)
                assertTrue("flyway_schema_history" in tables)

                val blogIdColumnTables =
                    listOf("posts", "pages", "tags", "media", "themes", "widgets", "settings")
                for (table in blogIdColumnTables) {
                    val rs =
                        stmt.executeQuery(
                            """
                            SELECT 1
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = '$table'
                              AND column_name = 'blog_id'
                            """.trimIndent(),
                        )
                    assertTrue(rs.next(), "$table must have blog_id column")
                }

                val defaultBlog =
                    stmt.executeQuery(
                        "SELECT slug FROM blogs WHERE slug = 'default'",
                    )
                assertTrue(defaultBlog.next())
                assertEquals("default", defaultBlog.getString("slug"))

                val readonlyRole =
                    stmt.executeQuery(
                        "SELECT 1 FROM pg_roles WHERE rolname = 'repost_public_readonly'",
                    )
                assertTrue(readonlyRole.next())
            }
        }
    }
}
