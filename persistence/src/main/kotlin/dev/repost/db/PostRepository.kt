package dev.repost.db

import dev.repost.db.tables.Blogs
import dev.repost.db.tables.Posts
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.UUID

object BlogIds {
    val DEFAULT: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
}

object PostRepository {
    fun findDefaultBlogId(): UUID? =
        transaction {
            Blogs
                .selectAll()
                .where { Blogs.slug eq "default" }
                .map { it[Blogs.id] }
                .singleOrNull()
                ?: BlogIds.DEFAULT.takeIf {
                    Blogs.selectAll().where { Blogs.id eq BlogIds.DEFAULT }.count() > 0
                }
        }

    fun listByBlog(
        blogId: UUID,
        status: String? = null,
    ): List<PostRecord> =
        transaction {
            Posts
                .selectAll()
                .where {
                    if (status != null) {
                        (Posts.blogId eq blogId) and (Posts.status eq status)
                    } else {
                        Posts.blogId eq blogId
                    }
                }.orderBy(Posts.updatedAt to SortOrder.DESC)
                .map { it.toPostRecord() }
        }

    fun findById(
        blogId: UUID,
        id: UUID,
    ): PostRecord? =
        transaction {
            Posts
                .selectAll()
                .where { (Posts.blogId eq blogId) and (Posts.id eq id) }
                .map { it.toPostRecord() }
                .singleOrNull()
        }

    fun findBySlug(
        blogId: UUID,
        slug: String,
    ): PostRecord? =
        transaction {
            Posts
                .selectAll()
                .where { (Posts.blogId eq blogId) and (Posts.slug eq slug) }
                .map { it.toPostRecord() }
                .singleOrNull()
        }

    fun create(
        blogId: UUID,
        write: PostWrite,
    ): PostRecord =
        transaction {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now()
            Posts.insert {
                it[Posts.id] = id
                it[Posts.blogId] = blogId
                it[slug] = write.slug
                it[title] = write.title
                it[excerpt] = write.excerpt
                it[contentMd] = write.contentMd
                it[status] = write.status
                it[publishedAt] = write.publishedAt
                it[readingTime] = estimateReadingTime(write.contentMd)
                it[createdAt] = now
                it[updatedAt] = now
            }
            findById(blogId, id)!!
        }

    fun update(
        blogId: UUID,
        id: UUID,
        write: PostWrite,
    ): PostRecord? =
        transaction {
            val updated =
                Posts.update({ (Posts.blogId eq blogId) and (Posts.id eq id) }) {
                    it[slug] = write.slug
                    it[title] = write.title
                    it[excerpt] = write.excerpt
                    it[contentMd] = write.contentMd
                    it[status] = write.status
                    it[publishedAt] = write.publishedAt
                    it[readingTime] = estimateReadingTime(write.contentMd)
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) null else findById(blogId, id)
        }

    fun delete(
        blogId: UUID,
        id: UUID,
    ): Boolean =
        transaction {
            Posts.deleteWhere { (Posts.blogId eq blogId) and (Posts.id eq id) } > 0
        }

    fun estimateReadingTime(markdown: String): Int {
        val words = markdown.split(Regex("\\s+")).count { it.isNotBlank() }
        return (words / 200.0).coerceAtLeast(1.0).toInt()
    }

    private fun ResultRow.toPostRecord() =
        PostRecord(
            id = this[Posts.id],
            blogId = this[Posts.blogId],
            slug = this[Posts.slug],
            title = this[Posts.title],
            excerpt = this[Posts.excerpt],
            contentMd = this[Posts.contentMd],
            coverMediaId = this[Posts.coverMediaId],
            status = this[Posts.status],
            publishedAt = this[Posts.publishedAt],
            readingTime = this[Posts.readingTime],
            createdAt = this[Posts.createdAt],
            updatedAt = this[Posts.updatedAt],
        )
}
