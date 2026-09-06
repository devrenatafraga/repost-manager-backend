package dev.repost.admin.posts

import kotlinx.serialization.Serializable

@Serializable
data class PostResponse(
    val id: String,
    val blogId: String,
    val slug: String,
    val title: String,
    val excerpt: String? = null,
    val contentMd: String,
    val coverMediaId: String? = null,
    val status: String,
    val publishedAt: String? = null,
    val readingTime: Int? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UpsertPostRequest(
    val slug: String,
    val title: String,
    val excerpt: String? = null,
    val contentMd: String = "",
    val status: String = "draft",
    val publishedAt: String? = null,
)

@Serializable
data class PostListResponse(
    val items: List<PostResponse>,
)
