# LexEase BE

Spring Boot 4 backend for LexEase.

## Local Development

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run tests:

```bash
./gradlew test
```

Run the app:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Promote the first admin after registering a user:

```bash
psql "$DB_URL" -v email='admin@example.com' -f scripts/promote-admin.sql
```
