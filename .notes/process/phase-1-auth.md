# Phase 1 - Auth And Authorization Process

## Implemented Features

- Public register/login/refresh/logout.
- `GET /me`.
- BCrypt password hashing.
- JWT access token signing and verification with Nimbus JOSE JWT.
- Opaque 256-bit refresh token generation.
- Refresh token hashing with HMAC-SHA256 and server-side pepper.
- Refresh token persistence, expiry, revoke, and rotation.
- Stateless Spring Security filter for `Authorization: Bearer <jwt>`.
- Disabled users are rejected during token authentication and refresh.
- Public registration allows `GUARDIAN` and `CHILD`.
- Public registration rejects `ADMIN`.
- SQL promote script for first admin: `scripts/promote-admin.sql`.
- Admin user listing and status update.
- Guardian-child link request/list/accept/reject/revoke.
- `PermissionService` for relation-based child access and story-management checks.
- Basic audit logs for sensitive actions.
- Domain constants are represented by enums instead of string literals:
  - `ErrorCode`
  - `AuditAction`
  - `AuditTargetType`
  - user/link role and status enums.

## Data Model

Implemented in `V1__init_auth.sql`:

- `users`
- `refresh_tokens`
- `guardian_child_links`
- `audit_logs`

Enums represented as checked text columns in PostgreSQL:

- `users.role`: `ADMIN`, `GUARDIAN`, `CHILD`
- `users.status`: `ACTIVE`, `DISABLED`
- `guardian_child_links.status`: `PENDING`, `ACCEPTED`, `REJECTED`, `REVOKED`

## Endpoint: `POST /auth/register`

Registers a public user and returns tokens.

Input:

```json
{
  "email": "child@example.com",
  "password": "password123",
  "displayName": "Be An",
  "role": "CHILD"
}
```

Input fields:

- `email`: required email, trimmed and lowercased before storage.
- `password`: required, 8-128 chars. Password is not trimmed and is stored only as a BCrypt hash.
- `displayName`: required display name, max 120 chars.
- `role`: required. Only `GUARDIAN` and `CHILD` are allowed publicly. `ADMIN` is rejected.

Output:

```json
{
  "accessToken": "jwt",
  "refreshToken": "opaque-random-token",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "email": "child@example.com",
    "displayName": "Be An",
    "role": "CHILD",
    "status": "ACTIVE"
  }
}
```

Output fields:

- `accessToken`: short-lived signed JWT for API calls.
- `refreshToken`: opaque token returned once to the client. Only its hash is stored.
- `expiresIn`: access token lifetime in seconds.
- `user`: registered user DTO.

Implementation:

- `AuthController` delegates to `AuthService.register`.
- Email is normalized to lowercase.
- Duplicate email returns `EMAIL_ALREADY_REGISTERED`.
- Password is encoded with `BCryptPasswordEncoder`.
- User is created with `ACTIVE` status.
- A refresh token row is created and an access token is signed.
- Audit action: `USER_REGISTERED`.

## Endpoint: `POST /auth/login`

Authenticates a user and returns tokens.

Input:

```json
{
  "email": "child@example.com",
  "password": "password123",
  "deviceId": "optional-device-id"
}
```

Input fields:

- `email`: required email, normalized before lookup.
- `password`: required password, compared against BCrypt hash.
- `deviceId`: optional client device identifier stored with refresh token.

Output: same shape as register.

Implementation:

- Unknown email, disabled user, and invalid password all return `INVALID_CREDENTIALS`.
- Successful login creates a new refresh token and signs a new access token.
- Audit action: `USER_LOGIN`.

## Endpoint: `POST /auth/refresh`

Rotates a refresh token and returns a new token pair.

Input:

```json
{
  "refreshToken": "opaque-random-token"
}
```

Input fields:

- `refreshToken`: required raw refresh token previously returned by register/login/refresh.

Output: same shape as register.

Implementation:

- Raw token is HMAC-hashed with `JWT_REFRESH_PEPPER`.
- Stored token must exist, not be revoked, and not be expired.
- Old refresh token is revoked.
- New refresh token is created with the same `deviceId`.
- Access token is reissued.
- Refresh token TTL is `30d`.
- Audit action: `REFRESH_TOKEN_ROTATED`.

## Endpoint: `POST /auth/logout`

Revokes a refresh token.

Input:

```json
{
  "refreshToken": "opaque-random-token"
}
```

Input fields:

- `refreshToken`: required raw refresh token to revoke.

Output: HTTP `204 No Content`.

Implementation:

- Hashes the raw refresh token and revokes the matching row if present.
- Missing token is treated as idempotent no-op.

## Endpoint: `GET /me`

Returns the authenticated user.

Input:

- Header: `Authorization: Bearer <accessToken>`.

Output:

```json
{
  "id": "uuid",
  "email": "child@example.com",
  "displayName": "Be An",
  "role": "CHILD",
  "status": "ACTIVE"
}
```

Output fields:

- `id`: public user UUID.
- `email`: normalized email.
- `displayName`: display name.
- `role`: `ADMIN`, `GUARDIAN`, or `CHILD`.
- `status`: `ACTIVE` or `DISABLED`.

Implementation:

- `JwtAuthenticationFilter` verifies bearer token signature, issuer, algorithm, and expiry.
- User is loaded from JWT `sub`.
- Disabled users are rejected.

## Endpoint: `GET /admin/users`

Lists users for admins.

Input:

- Header: `Authorization: Bearer <adminAccessToken>`.
- Query params supported by Spring Pageable:
  - `page`: zero-based page index.
  - `size`: page size.
  - `sort`: sort expression.

Output:

```json
{
  "items": [
    {
      "id": "uuid",
      "email": "child@example.com",
      "displayName": "Be An",
      "role": "CHILD",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

Implementation:

- Protected by `@PreAuthorize("hasRole('ADMIN')")`.
- Returns `PageResponse<UserResponse>`.

## Endpoint: `PATCH /admin/users/{id}/status`

Updates user status for admins.

Input:

```json
{
  "status": "DISABLED"
}
```

Input fields:

- `status`: required. `ACTIVE` or `DISABLED`.

Output: `UserResponse`.

Implementation:

- Protected by admin role.
- Loads user by path UUID.
- Updates status and `updated_at`.

## Endpoint: `POST /guardian-child-links`

Guardian requests a link to a child by child email.

Input:

```json
{
  "childEmail": "child@example.com"
}
```

Input fields:

- `childEmail`: required email of an existing active child user.

Output:

```json
{
  "linkId": "uuid",
  "guardianId": "uuid",
  "childId": "uuid",
  "status": "PENDING"
}
```

Implementation:

- Current user must be `GUARDIAN`.
- Target user must exist, have role `CHILD`, and status `ACTIVE`.
- Existing non-revoked pair is rejected.
- Link starts as `PENDING`.
- Audit action: `GUARDIAN_CHILD_LINK_REQUESTED`.

## Endpoint: `GET /guardian-child-links`

Lists links where current user is guardian or child.

Input:

- Header: `Authorization: Bearer <accessToken>`.

Output:

```json
[
  {
    "linkId": "uuid",
    "guardianId": "uuid",
    "childId": "uuid",
    "status": "PENDING"
  }
]
```

Implementation:

- Queries by current user ID as guardian or child.

## Endpoint: `POST /guardian-child-links/{id}/accept`

Accepts a pending guardian-child link.

Input:

- Path `id`: link UUID.
- Header: `Authorization: Bearer <accessToken>`.

Output: `GuardianChildLinkResponse`.

Implementation:

- Link must be `PENDING`.
- Allowed actors:
  - `ADMIN`
  - the child in the link
  - any current accepted guardian of the child
- Sets status `ACCEPTED` and `accepted_at`.
- This matches the decision that only one current guardian approval is enough.
- Audit action: `GUARDIAN_CHILD_LINK_ACCEPTED`.

## Endpoint: `POST /guardian-child-links/{id}/reject`

Rejects a pending guardian-child link.

Input: same as accept.

Output: `GuardianChildLinkResponse`.

Implementation:

- Same actor rule as accept.
- Sets status `REJECTED`.
- Audit action: `GUARDIAN_CHILD_LINK_REJECTED`.

## Endpoint: `DELETE /guardian-child-links/{id}`

Revokes a guardian-child link.

Input:

- Path `id`: link UUID.
- Header: `Authorization: Bearer <accessToken>`.

Output: HTTP `204 No Content`.

Implementation:

- Allowed actors:
  - `ADMIN`
  - the child in the link
  - the guardian in the link
  - any accepted guardian of the child
- Sets status `REVOKED`.
- Audit action: `GUARDIAN_CHILD_LINK_REVOKED`.

## Security Implementation

- API is stateless.
- CSRF is disabled for API usage.
- Public endpoints:
  - `/auth/register`
  - `/auth/login`
  - `/auth/refresh`
  - `/actuator/health`
  - `/actuator/info`
- Every other endpoint requires bearer JWT.
- JWT claims:
  - `sub`: user UUID.
  - `role`: user role.
  - `iat`: issued-at.
  - `exp`: expiry.
  - `jti`: unique token id.
- JWT issuer defaults to `lexease`.
- HS256 is enforced.

## Admin Promotion

First admin is not bootstrapped by application startup.

Flow:

1. Register a normal user.
2. Run:

```bash
psql "$DB_URL" -v email='admin@example.com' -f scripts/promote-admin.sql
```

The script promotes the user with matching email to `ADMIN`.

## Error Shape

Errors use:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "details": []
}
```

Validation errors include field-level entries in `details`.

Implementation:

- API error codes are defined in `ErrorCode`.
- Services throw `ApiException` with an `ErrorCode`, not a raw string.
- Audit logs are written with `AuditAction` and `AuditTargetType`, not raw string values.

## Verification

- `./gradlew test` passes.
- Added unit tests for:
  - public admin registration rejection.
  - invalid login rejection.
  - relation-based permission checks.
- Local app smoke started successfully against Docker PostgreSQL.
- Migration history was later squashed to the current `V1`-`V3` set. Auth schema remains in `V1__init_auth.sql`.
