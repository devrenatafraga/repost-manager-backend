package dev.repost.publicapi.posts

import dev.repost.db.DatabaseFactory
import dev.repost.db.PostRecord
import dev.repost.db.PostRepository
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.configurePublicPostRoutes() {
    route("/api/v1/public/posts") {
        get({
            tags = listOf("public")
            summary = "List published posts"
            description = "Anonymous list of published posts for the default blog, newest first."
            response {
                HttpStatusCode.OK to {
                    description = "Published posts"
                    body<PublicPostListResponse>()
                }
                HttpStatusCode.ServiceUnavailable to {
                    description = "Database or default blog unavailable"
                }
            }
        }) {
            val blogId =
                requireDefaultBlogId()
                    ?: return@get call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "unavailable"))
            val items = PostRepository.listPublished(blogId).map { it.toPublicResponse() }
            call.respond(PublicPostListResponse(items))
        }

        get("/{slug}", {
            tags = listOf("public")
            summary = "Get published post by slug"
            request {
                pathParameter<String>("slug")
            }
            response {
                HttpStatusCode.OK to {
                    description = "Published post"
                    body<PublicPostResponse>()
                }
                HttpStatusCode.NotFound to {
                    description = "Missing or not published"
                }
                HttpStatusCode.ServiceUnavailable to {
                    description = "Database or default blog unavailable"
                }
            }
        }) {
            val blogId =
                requireDefaultBlogId()
                    ?: return@get call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "unavailable"))
            val slug =
                call.parameters["slug"]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_slug"))
            val post =
                PostRepository.findPublishedBySlug(blogId, slug)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
            call.respond(post.toPublicResponse())
        }
    }
}

private fun requireDefaultBlogId() =
    if (!DatabaseFactory.isConnected()) {
        null
    } else {
        PostRepository.findDefaultBlogId()
    }

private fun PostRecord.toPublicResponse() =
    PublicPostResponse(
        id = id.toString(),
        slug = slug,
        title = title,
        excerpt = excerpt,
        contentMd = contentMd,
        coverMediaId = coverMediaId?.toString(),
        publishedAt = publishedAt?.toString(),
        readingTime = readingTime,
        updatedAt = updatedAt.toString(),
    )
