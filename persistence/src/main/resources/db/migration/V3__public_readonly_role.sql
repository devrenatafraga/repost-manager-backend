-- Read-only role for the /public API surface (ADR-0002, ADR-0005)
-- Password must be rotated via env/secret management ADR before production use.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'repost_public_readonly') THEN
        CREATE ROLE repost_public_readonly LOGIN PASSWORD 'repost_public_readonly_dev';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE repost TO repost_public_readonly;
GRANT USAGE ON SCHEMA public TO repost_public_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO repost_public_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO repost_public_readonly;
