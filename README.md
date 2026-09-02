# repost-manager-backend

API do Repost (Kotlin + Ktor): superfícies `/api/v1/admin/**` e `/api/v1/public/**`.

Decisões de arquitetura: [repost-documentation](https://github.com/devrenatafraga/repost-documentation/tree/main/docs/adr).

## Módulos

| Módulo | Papel |
| --- | --- |
| `server` | Aplicação Ktor (entrypoint) |
| `admin-api` | Rotas e lógica da superfície admin |
| `public-api` | Rotas e lógica da superfície pública (sem dependência de `admin-api`) |

## Desenvolvimento local

Requisitos: **JDK 25 (LTS)** e **Gradle 9.7.0** (via wrapper `./gradlew`).

```bash
./gradlew build
./gradlew :server:run
```

Endpoints iniciais:

- `GET /health` — healthcheck
- `GET /api/v1/public/` — stub da superfície pública
- `GET /api/v1/admin/` — stub da superfície admin

## Licença

MIT
