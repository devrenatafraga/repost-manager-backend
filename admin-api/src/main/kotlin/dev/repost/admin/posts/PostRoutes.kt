package dev.repost.admin.posts

import dev.repost.db.PostRecord
import dev.repost.db.PostRepository
import dev.repost.db.PostWrite
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

private val ALLOWED_STATUSES = setOf("draft", "scheduled", "published")

fun Route.configurePostRoutes() {
    route("/api/v1/admin/posts") {
        get({
            tags = listOf("admin")
            summary = "List admin posts"
            request {
                queryParameter<String>("status") {
                    description = "Optional filter: draft | scheduled | published"
                    required = false
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Posts for the default blog"
                    body<PostListResponse>()
                }
            }
        }) {
            val blogId = requireDefaultBlogId() ?: return@get call.respond(HttpStatusCode.ServiceUnavailable)
            val status = call.request.queryParameters["status"]
            if (status != null && status !in ALLOWED_STATUSES) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_status"))
            }
            val items = PostRepository.listByBlog(blogId, status).map { it.toResponse() }
            call.respond(PostListResponse(items))
        }

        post({
            tags = listOf("admin")
            summary = "Create post"
            request { body<UpsertPostRequest>() }
            response {
                HttpStatusCode.Created to {
                    description = "Created"
                    body<PostResponse>()
                }
                HttpStatusCode.BadRequest to { description = "Validation error" }
                HttpStatusCode.Conflict to { description = "Slug already exists" }
            }
        }) {
            val blogId = requireDefaultBlogId() ?: return@post call.respond(HttpStatusCode.ServiceUnavailable)
            val body = call.receive<UpsertPostRequest>()
            val write =
                body.toWrite()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_payload"))
            if (PostRepository.findBySlug(blogId, write.slug) != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "slug_taken"))
            }
            val created = PostRepository.create(blogId, write)
            call.respond(HttpStatusCode.Created, created.toResponse())
        }

        get("/{id}", {
            tags = listOf("admin")
            summary = "Get post by id"
            request {
                pathParameter<String>("id")
            }
            response {
                HttpStatusCode.OK to {
                    description = "Post"
                    body<PostResponse>()
                }
                HttpStatusCode.NotFound to { description = "Not found" }
            }
        }) {
            val blogId = requireDefaultBlogId() ?: return@get call.respond(HttpStatusCode.ServiceUnavailable)
            val id =
                call.parameters["id"]?.toUuidOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_id"))
            val post =
                PostRepository.findById(blogId, id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
            call.respond(post.toResponse())
        }

        put("/{id}", {
            tags = listOf("admin")
            summary = "Update post"
            request {
                pathParameter<String>("id")
                body<UpsertPostRequest>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Updated"
                    body<PostResponse>()
                }
                HttpStatusCode.NotFound to { description = "Not found" }
                HttpStatusCode.Conflict to { description = "Slug already exists" }
            }
        }) {
            val blogId = requireDefaultBlogId() ?: return@put call.respond(HttpStatusCode.ServiceUnavailable)
            val id =
                call.parameters["id"]?.toUuidOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_id"))
            val body = call.receive<UpsertPostRequest>()
            val write =
                body.toWrite()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_payload"))
            val slugOwner = PostRepository.findBySlug(blogId, write.slug)
            if (slugOwner != null && slugOwner.id != id) {
                return@put call.respond(HttpStatusCode.Conflict, mapOf("error" to "slug_taken"))
            }
            val updated =
                PostRepository.update(blogId, id, write)
                    ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
            call.respond(updated.toResponse())
        }

        delete("/{id}", {
            tags = listOf("admin")
            summary = "Delete post"
            request {
                pathParameter<String>("id")
            }
            response {
                HttpStatusCode.NoContent to { description = "Deleted" }
                HttpStatusCode.NotFound to { description = "Not found" }
            }
        }) {
            val blogId = requireDefaultBlogId() ?: return@delete call.respond(HttpStatusCode.ServiceUnavailable)
            val id =
                call.parameters["id"]?.toUuidOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_id"))
            if (!PostRepository.delete(blogId, id)) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun requireDefaultBlogId(): UUID? = PostRepository.findDefaultBlogId()

private fun UpsertPostRequest.toWrite(): PostWrite? {
    val normalizedSlug = slug.trim().lowercase()
    val normalizedTitle = title.trim()
    val normalizedStatus = status.trim().lowercase()
    if (normalizedSlug.isEmpty() || normalizedTitle.isEmpty()) return null
    if (normalizedStatus !in ALLOWED_STATUSES) return null
    if (!normalizedSlug.matches(Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$"))) return null

    val published =
        when {
            publishedAt != null ->
                try {
                    OffsetDateTime.parse(publishedAt)
                } catch (_: DateTimeParseException) {
                    return null
                }
            normalizedStatus == "published" -> OffsetDateTime.now()
            else -> null
        }

    return PostWrite(
        slug = normalizedSlug,
        title = normalizedTitle,
        excerpt = excerpt?.trim()?.ifEmpty { null },
        contentMd = contentMd,
        status = normalizedStatus,
        publishedAt = published,
    )
}

private fun PostRecord.toResponse() =
    PostResponse(
        id = id.toString(),
        blogId = blogId.toString(),
        slug = slug,
        title = title,
        excerpt = excerpt,
        contentMd = contentMd,
        coverMediaId = coverMediaId?.toString(),
        status = status,
        publishedAt = publishedAt?.toString(),
        readingTime = readingTime,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

private fun String.toUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        null
    }
