package dev.repost.publicapi.posts

import kotlinx.serialization.Serializable

@Serializable
data class PublicPostResponse(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String? = null,
    val contentMd: String,
    val coverMediaId: String? = null,
    val publishedAt: String? = null,
    val readingTime: Int? = null,
    val updatedAt: String,
)

@Serializable
data class PublicPostListResponse(
    val items: List<PublicPostResponse>,
)
