package dev.repost.db

object AdminSeeder {
    fun seedFromEnvironment() {
        val email = System.getenv("ADMIN_EMAIL")?.takeIf { it.isNotBlank() } ?: return
        val passwordHash = System.getenv("ADMIN_PASSWORD_HASH")?.takeIf { it.isNotBlank() } ?: return
        val user = UserRepository.upsertAdmin(email, passwordHash)
        println("Admin seeded/updated: ${user.email} (${user.id})")
    }
}
