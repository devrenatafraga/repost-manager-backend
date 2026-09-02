-- Seed default blog for single-tenant MVP (ADR-0005)
-- Password hash is a placeholder until auth ADR is implemented.

INSERT INTO users (id, email, password_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@repost.local',
    '$2a$10$PLACEHOLDER.CHANGE.WITH.AUTH.ADR',
    'admin'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO blogs (id, owner_id, slug, title, description, locale, timezone)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'default',
    'Repost',
    'Default blog for single-tenant MVP',
    'pt-BR',
    'America/Sao_Paulo'
)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO settings (blog_id, data)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '{}'::jsonb
)
ON CONFLICT (blog_id) DO NOTHING;
