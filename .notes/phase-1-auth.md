# Phase 1 - Auth And Authorization

Mục tiêu: tự cài auth đơn giản nhưng đủ an toàn cho MVP, gồm register/login/refresh/logout, hash password, tự sign token, role và toàn bộ phân quyền liên quan.

Không làm trong phase này:

- Forgot password.
- Verify email.
- Social login.
- Keycloak/Firebase/Supabase Auth.

## Input

Register:

```json
{
  "email": "child@example.com",
  "password": "********",
  "displayName": "Be An",
  "role": "CHILD"
}
```

Login:

```json
{
  "email": "child@example.com",
  "password": "********",
  "deviceId": "optional-device-id"
}
```

Refresh:

```json
{
  "refreshToken": "opaque-random-token"
}
```

Guardian link child:

```json
{
  "childEmail": "child@example.com"
}
```

## Output

Login/register success:

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

`GET /me`:

```json
{
  "id": "uuid",
  "email": "parent@example.com",
  "displayName": "Me An",
  "role": "GUARDIAN",
  "status": "ACTIVE"
}
```

Guardian-child link:

```json
{
  "linkId": "uuid",
  "guardianId": "uuid",
  "childId": "uuid",
  "status": "PENDING"
}
```

## Data Model

`users`

- `id uuid primary key`
- `email text not null unique`
- `password_hash text not null`
- `display_name text not null`
- `role text not null check in ('ADMIN','GUARDIAN','CHILD')`
- `status text not null check in ('ACTIVE','DISABLED')`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`refresh_tokens`

- `id uuid primary key`
- `user_id uuid not null references users(id)`
- `token_hash text not null unique`
- `device_id text null`
- `expires_at timestamptz not null`
- `revoked_at timestamptz null`
- `created_at timestamptz not null`

`guardian_child_links`

- `id uuid primary key`
- `guardian_id uuid not null references users(id)`
- `child_id uuid not null references users(id)`
- `status text not null check in ('PENDING','ACCEPTED','REJECTED','REVOKED')`
- `invited_by uuid not null references users(id)`
- `created_at timestamptz not null`
- `accepted_at timestamptz null`
- unique active pair: `(guardian_id, child_id)` khi status chưa `REVOKED`

`audit_logs`

- `id uuid primary key`
- `actor_user_id uuid null`
- `action text not null`
- `target_type text not null`
- `target_id uuid null`
- `metadata jsonb not null default '{}'`
- `created_at timestamptz not null`

## Implementation

### 1.1 Password hashing

- Dùng `BCryptPasswordEncoder` hoặc `Argon2PasswordEncoder`.
- Không dùng SHA/MD5.
- Password policy MVP: min length 8, max length hợp lý, trim email nhưng không trim password.

### 1.2 Token

Access token:

- JWT signed bằng secret từ env.
- TTL 10-15 phút.
- Claims tối thiểu: `sub`, `role`, `iat`, `exp`, `jti`.
- Không nhét dữ liệu nhạy cảm vào JWT.

Refresh token:

- Opaque random token 256-bit.
- Chỉ trả raw token cho client một lần.
- Lưu hash trong DB.
- TTL 30 ngày.
- Rotate khi refresh: revoke token cũ, tạo token mới.

### 1.3 Spring Security

- Stateless API.
- Custom filter đọc `Authorization: Bearer`.
- Verify signature/expiry/issuer/algorithm.
- Load current user từ `sub`.
- Reject user `DISABLED`.

### 1.4 Role permissions

Role chính:

- `ADMIN`
- `GUARDIAN`
- `CHILD`

Permission matrix MVP:

| Capability | ADMIN | GUARDIAN | CHILD |
|---|---:|---:|---:|
| Manage users | yes | no | no |
| Create/update/delete stories | yes | no | no |
| Search published stories | yes | yes | yes |
| Link child | no | yes | no |
| Accept guardian link | yes | conditional | conditional |
| Block story for child | no | accepted child only | no |
| Read own story session | no | no | own only |
| View child progress | yes | accepted child only | own summary optional |
| Manage child display settings | yes | accepted child only | own optional |
| Manage reminders | yes | accepted child only | no |

### 1.5 Relation-based authorization

Tạo `AuthorizationService` hoặc `PermissionService`:

- `canAccessChild(currentUser, childId)`
- `canManageChild(currentUser, childId)`
- `canViewRecording(currentUser, recordingId)`
- `canViewStory(currentUser, storyId)`
- `canManageStory(currentUser)`

Không rải query permission thủ công trong controller.

### 1.6 Admin bootstrap

User public không được tự chọn `ADMIN`.

Decision:

- Public register không được tạo `ADMIN`.
- Cung cấp SQL script `scripts/promote-admin.sql` để promote một user đã register lên `ADMIN`.
- App không tự bootstrap admin qua env/startup.

### 1.7 Guardian-child linking

Flow đề xuất:

1. Guardian nhập email child.
2. Backend tìm user role `CHILD`.
3. Tạo link `PENDING`.
4. Nếu child chưa có guardian, child tự accept sau login.
5. Nếu child đã có guardian, một current guardian accepted hoặc child accept.

Decision: nếu child đã có guardian, chỉ cần một current guardian accepted hoặc child accept để approve link mới.

## APIs

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `GET /admin/users`
- `PATCH /admin/users/{id}/status`
- `POST /guardian-child-links`
- `GET /guardian-child-links`
- `POST /guardian-child-links/{id}/accept`
- `POST /guardian-child-links/{id}/reject`
- `DELETE /guardian-child-links/{id}`

## Done Criteria

- Register/login/refresh/logout hoạt động.
- Password hash trong DB, không lưu plain text.
- Refresh token lưu hash, revoke được.
- Admin không thể được tạo qua public register.
- API protected reject token invalid/expired.
- Relation-based permission có test cho guardian-child accepted/rejected/revoked.
