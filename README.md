# repost-manager-backend

API do Repost (Kotlin + Ktor): superfícies `/api/v1/admin/**` e `/api/v1/public/**`.

Decisões de arquitetura: [repost-documentation](https://github.com/devrenatafraga/repost-documentation/tree/main/docs/adr).

## Módulos

| Módulo | Papel |
| --- | --- |
| `server` | Aplicação Ktor (entrypoint) |
| `admin-api` | Rotas e lógica da superfície admin |
| `public-api` | Rotas e lógica da superfície pública (sem dependência de `admin-api`) |
| `persistence` | Migrations Flyway e acesso ao Postgres |

## Desenvolvimento local

Requisitos: **JDK 25 (LTS)**, **Gradle 9.7.0** (wrapper), **Docker** (Postgres local).

Kotlin **2.3.20** ([release notes](https://kotlinlang.org/docs/whatsnew2320.html)).

### 1. Subir Postgres local

```bash
docker compose up -d
cp .env.example .env
```

### 2. Rodar migrations

Com o Postgres no ar:

```bash
./gradlew :persistence:flywayMigrate
```

Ou subir o servidor (aplica migrations na inicialização se `DATABASE_URL` estiver definida):

```bash
set -a && source .env && set +a
./gradlew :server:run
```

### 3. Build e testes

```bash
./gradlew build
```

Os testes de migration usam **Testcontainers** (Postgres efêmero) — a CI no GitHub Actions valida o mesmo fluxo sem banco externo.

## Endpoints iniciais

- `GET /health` — healthcheck (funciona sem `DATABASE_URL`)
- `GET /openapi.json` — contrato OpenAPI
- `GET /api/v1/public/` — stub da superfície pública
- `GET /api/v1/public/posts` — lista posts `published` (anônimo)
- `GET /api/v1/public/posts/{slug}` — post publicado por slug (404 se draft/inexistente)
- `GET /api/v1/admin/` — stub da superfície admin (**exige** `Authorization: Bearer` quando auth está configurada)
- `POST /api/v1/admin/auth/login` — login (JWT + cookie refresh; rate limit por IP + email)
- `POST /api/v1/admin/auth/refresh` — renova access token (rate limit por IP)
- `POST /api/v1/admin/auth/logout` — encerra sessão
- `GET/POST /api/v1/admin/posts` e `GET/PUT/DELETE /api/v1/admin/posts/{id}` — CRUD de posts (JWT)

Auth exige `DATABASE_URL`, `JWT_SECRET` (≥32 chars), `ADMIN_EMAIL` e `ADMIN_PASSWORD_HASH` (BCrypt). CORS com credentials usa `CORS_ORIGINS` (padrão `http://localhost:5173`). Ver `.env.example`.

## Deploy (Koyeb)

Imagem em [`Dockerfile`](Dockerfile): build Gradle e runtime **JRE 25**. A distribuição já sobe com `-Xmx256m -XX:MaxMetaspaceSize=96m` para caber na instância gratuita (~512 MB). O processo escuta `0.0.0.0` e a porta `PORT` (padrão 8080). Health check: `GET /health`.

No serviço Koyeb, builder **Dockerfile**, região free (Frankfurt ou Washington), e estas variáveis. Não commite os valores.

| Variável | Obrigatória | Notas |
| --- | --- | --- |
| `DATABASE_URL` | sim | JDBC, **sem** usuário/senha na URL. Ex.: `jdbc:postgresql://HOST/neondb?sslmode=require` |
| `DATABASE_USER` | sim | Usuário do Neon |
| `DATABASE_PASSWORD` | sim | Senha do Neon |
| `JWT_SECRET` | sim | Pelo menos 32 caracteres |
| `ADMIN_EMAIL` | sim | Email do admin |
| `ADMIN_PASSWORD_HASH` | sim | Hash BCrypt, não a senha em texto |
| `CORS_ORIGINS` | sim | Origem do manager (URL da Vercel), separada por vírgula |
| `API_BASE_URL` | sim | URL pública `https://…koyeb.app` |
| `COOKIE_SECURE` | sim | `true` em HTTPS |
| `PORT` | não | O Koyeb define |

Na subida, o Flyway aplica as migrations se o banco estiver configurado. O schema também pode ser aplicado antes, no Mac, com `./gradlew :persistence:flywayMigrate`.

## Licença

MIT
