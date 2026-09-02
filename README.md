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
- `GET /api/v1/admin/` — stub da superfície admin

## Banco na nuvem

Provisionamento (Neon, secrets de deploy) fica para quando a app for publicada. O schema já está versionado em `persistence/src/main/resources/db/migration/`.

## Licença

MIT
