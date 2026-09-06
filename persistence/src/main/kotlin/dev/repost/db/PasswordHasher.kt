package dev.repost.db

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    fun hash(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt(10))

    fun matches(
        plain: String,
        hash: String,
    ): Boolean = BCrypt.checkpw(plain, hash)
}
