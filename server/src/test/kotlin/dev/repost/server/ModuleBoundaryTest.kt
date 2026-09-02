package dev.repost.server

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Compile-time boundary: public-api must not depend on admin-api (ADR-0002).
 * This test reads Gradle metadata as a lightweight guard until dependency analysis is automated.
 */
class ModuleBoundaryTest {
    @Test
    fun `public-api build does not reference admin-api`() {
        val publicBuild = projectRoot().resolve("public-api/build.gradle.kts").readText()
        assertFalse(
            publicBuild.contains("admin-api"),
            "public-api must not depend on admin-api at compile time",
        )
    }

    private fun projectRoot() =
        checkNotNull(javaClass.classLoader.getResource(".")) {
            "Could not resolve project root from test classpath"
        }.toURI().let { uri ->
            // test classes live under server/build/...; walk up to repo root in CI/local
            val fromCwd = java.io.File(System.getProperty("user.dir"))
            if (fromCwd.resolve("public-api/build.gradle.kts").exists()) {
                fromCwd
            } else {
                fromCwd.parentFile ?: fromCwd
            }
        }
}
