package dev.repost.admin.auth

import java.util.concurrent.ConcurrentHashMap

/**
 * Fixed-window rate limiter keyed by arbitrary strings (e.g. IP + email).
 */
class AuthRateLimiter(
    private val maxAttempts: Int = 10,
    private val windowMs: Long = 60_000,
) {
    private data class Bucket(
        var count: Int,
        val windowStart: Long,
    )

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        while (true) {
            val existing = buckets[key]
            if (existing == null || now - existing.windowStart >= windowMs) {
                val fresh = Bucket(1, now)
                if (existing == null) {
                    if (buckets.putIfAbsent(key, fresh) == null) return true
                } else if (buckets.replace(key, existing, fresh)) {
                    return true
                }
                continue
            }
            if (existing.count >= maxAttempts) return false
            val updated = Bucket(existing.count + 1, existing.windowStart)
            if (buckets.replace(key, existing, updated)) return true
        }
    }
}
