package dev.repost.db

import java.time.OffsetDateTime
import java.util.UUID

data class PostRecord(
    val id: UUID,
    val blogId: UUID,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val contentMd: String,
    val coverMediaId: UUID?,
    val status: String,
    val publishedAt: OffsetDateTime?,
    val readingTime: Int?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class PostWrite(
    val slug: String,
    val title: String,
    val excerpt: String?,
    val contentMd: String,
    val status: String,
    val publishedAt: OffsetDateTime?,
)
