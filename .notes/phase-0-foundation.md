# Phase 0 - Foundation

Mục tiêu: tạo nền repo/backend từ con số 0 để các phase sau có cùng chuẩn build, schema, config và test.

## Input

- Java 21.
- Spring Boot stable mới nhất tại thời điểm tạo project.
- PostgreSQL.
- Flyway.
- Hibernate/JPA.
- Repo hiện tại gần như trống.

## Output

- Spring Boot application chạy được local.
- Docker Compose dev có PostgreSQL.
- Flyway migration baseline.
- Health endpoint.
- Cấu trúc package theo domain.
- Test setup tối thiểu.

## Implementation

### 0.1 Bootstrap project

Tạo project Gradle và giữ nhất quán trong toàn repo.

Dependencies tối thiểu:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `flyway-core`
- PostgreSQL JDBC driver
- JWT library: Nimbus JOSE JWT hoặc JJWT
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`
- Testcontainers PostgreSQL cho integration test

### 0.2 Config profiles

Tạo:

- `application.yml`
- `application-local.yml`
- `application-test.yml`

Các config nhạy cảm lấy từ env:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_PEPPER`
- `OBJECT_STORAGE_MODE`
- `GEMINI_API_KEY`

### 0.3 Database baseline

Flyway:

- `V1__init_auth.sql`
- `V2__init_stories.sql`
- `V3__init_display_settings.sql`
- `V4__init_notifications.sql`
- `V5__init_reading_tts.sql`
- `V6__init_recordings_gemini.sql`

Không cần tạo hết chi tiết ngay trong Phase 0, nhưng đặt naming convention từ đầu.

### 0.4 Shared API conventions

Response lỗi thống nhất:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "details": []
}
```

Pagination:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 120,
  "totalPages": 6
}
```

### 0.5 Local dev

`docker-compose.yml` tối thiểu:

- PostgreSQL.

Object storage giai đoạn đầu có thể dùng filesystem local qua interface `ObjectStorageService`, sau này đổi MinIO/S3 mà không đổi business code.

### 0.6 Testing baseline

- Unit test cho service.
- `@WebMvcTest` cho controller.
- `@DataJpaTest` hoặc Testcontainers cho repository/query quan trọng.
- Một integration test smoke: app context + Flyway migration + `/actuator/health`.

## Done Criteria

- `./gradlew test` chạy pass.
- App local connect được PostgreSQL.
- Flyway chạy migration.
- Có global exception handler.
- Có DTO validation mẫu.
