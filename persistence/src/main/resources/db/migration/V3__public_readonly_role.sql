-- Read-only role for the /public API surface (ADR-0002, ADR-0005)
-- Password must be rotated via env/secret management ADR before production use.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'repost_public_readonly') THEN
        CREATE ROLE repost_public_readonly LOGIN PASSWORD 'repost_public_readonly_dev';
    END IF;
END
$$;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO repost_public_readonly', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO repost_public_readonly;
GRANT SELECT ON
    blogs,
    posts,
    pages,
    tags,
    post_tags,
    media,
    themes,
    widgets,
    settings
    TO repost_public_readonly;
