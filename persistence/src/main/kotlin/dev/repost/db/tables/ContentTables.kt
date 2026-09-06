package dev.repost.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

object Blogs : Table("blogs") {
    val id = uuid("id")
    val ownerId = uuid("owner_id")
    val slug = varchar("slug", 100)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val locale = varchar("locale", 10)
    val timezone = varchar("timezone", 64)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object Posts : Table("posts") {
    val id = uuid("id")
    val blogId = uuid("blog_id").references(Blogs.id)
    val slug = varchar("slug", 200)
    val title = varchar("title", 500)
    val excerpt = text("excerpt").nullable()
    val contentMd = text("content_md")
    val coverMediaId = uuid("cover_media_id").nullable()
    val status = varchar("status", 20)
    val publishedAt = timestampWithTimeZone("published_at").nullable()
    val readingTime = integer("reading_time").nullable()
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val updatedAt = timestampWithTimeZone("updated_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}
