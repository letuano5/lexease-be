# Phase 0 - Foundation Process

## Implemented Features

- Spring Boot project scaffolded with Gradle Wrapper.
- Java source/target compatibility set to Java 21; local build can run on newer JDK using `options.release = 21`.
- Dependencies added:
  - Web MVC
  - Validation
  - Spring Security
  - Spring Data JPA
  - Flyway
  - PostgreSQL JDBC
  - Nimbus JOSE JWT
  - Actuator
  - Spring Boot test starters
  - Testcontainers dependencies
  - H2 runtime for lightweight test profile
- Config profiles added:
  - `application.yml`
  - `application-local.yml`
  - `application-test.yml`
- Docker Compose dev PostgreSQL service added using `postgres:latest`.
- Current Flyway migration set after squashing implemented phases:
  - `V1__init_auth.sql`
  - `V2__init_stories.sql`
  - `V3__init_display_settings.sql`
- Shared API conventions added:
  - `ApiError`
  - `ApiException`
  - `GlobalExceptionHandler`
  - `PageResponse`
- Jackson config added for consistent JSON response writing.
- `Clock` bean added for testable time access.
- Health endpoint exposed through Actuator.

## Endpoints

### `GET /actuator/health`

Health check endpoint provided by Spring Boot Actuator.

Input: none.

Output:

```json
{
  "status": "UP"
}
```

Fields:

- `status`: current application health status. `UP` means the app is alive and required health contributors are healthy.

## Config

### Main profile

Important env-backed config:

- `DB_URL`: PostgreSQL JDBC URL. Default: `jdbc:postgresql://localhost:5432/lexease`.
- `DB_USERNAME`: database username. Default: `lexease`.
- `DB_PASSWORD`: database password. Default: `lexease`.
- `JWT_ACCESS_SECRET`: HS256 access token secret.
- `JWT_ACCESS_TOKEN_TTL`: access token TTL. Default: `15m`.
- `JWT_REFRESH_PEPPER`: HMAC pepper for hashing refresh tokens.
- `JWT_REFRESH_TOKEN_TTL`: refresh token TTL. Default: `30d`.
- `OBJECT_STORAGE_MODE`: storage mode placeholder. Default: `local`.
- `GEMINI_API_KEY`: Gemini API key placeholder.

### Test profile

`application-test.yml` uses H2 in PostgreSQL compatibility mode with Flyway disabled and Hibernate `create-drop`.

Reason: fast context/unit smoke tests should not require Docker. PostgreSQL/Flyway is verified separately through local Docker smoke.

## Implementation Notes

- Schema is owned by Flyway in local/main profiles. Hibernate uses `ddl-auto=validate`.
- The test profile intentionally uses Hibernate-generated schema because it is for lightweight app context tests, not migration validation.
- Docker Compose maps PostgreSQL to localhost port `5432`.
- `postgres:latest` is used because the project owner explicitly wants latest PostgreSQL image.

## Verification

- `./gradlew test` passes.
- `docker compose up -d postgres` started PostgreSQL from `postgres:latest`.
- `./gradlew bootRun --args='--spring.profiles.active=local'` started successfully against PostgreSQL.
- After migration squash, local/dev databases with old history must be reset with `docker compose down -v` or `drop schema public cascade; create schema public;` before running the app.
