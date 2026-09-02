-- ADR-0005: initial schema with blog_id on all content tables

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'admin',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE blogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users (id),
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    locale VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    alt TEXT,
    width INTEGER,
    height INTEGER,
    mime VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    slug VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    excerpt TEXT,
    content_md TEXT NOT NULL DEFAULT '',
    cover_media_id UUID REFERENCES media (id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'scheduled', 'published')),
    published_at TIMESTAMPTZ,
    reading_time INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blog_id, slug)
);

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blog_id, slug)
);

CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    slug VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    excerpt TEXT,
    content_md TEXT NOT NULL DEFAULT '',
    cover_media_id UUID REFERENCES media (id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'scheduled', 'published')),
    published_at TIMESTAMPTZ,
    reading_time INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blog_id, slug)
);

CREATE TABLE themes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    tokens JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blog_id, name)
);

CREATE TABLE widgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id UUID NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
    slot VARCHAR(20) NOT NULL CHECK (slot IN ('header', 'sidebar', 'footer')),
    type VARCHAR(50) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE settings (
    blog_id UUID PRIMARY KEY REFERENCES blogs (id) ON DELETE CASCADE,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_blog_id ON posts (blog_id);
CREATE INDEX idx_posts_blog_status_published ON posts (blog_id, status, published_at DESC);
CREATE INDEX idx_pages_blog_id ON pages (blog_id);
CREATE INDEX idx_tags_blog_id ON tags (blog_id);
CREATE INDEX idx_widgets_blog_slot_position ON widgets (blog_id, slot, position);
