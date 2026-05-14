# Phase 3 - Display Settings

Mục tiêu: lưu và áp dụng cấu hình hiển thị mặc định cho từng child để hỗ trợ trẻ khó đọc.

## Input

Get settings:

```http
GET /children/{childId}/display-settings
```

Save settings:

```json
{
  "fontFamily": "OpenDyslexic",
  "fontSize": 24,
  "lineHeight": 1.9,
  "letterSpacing": 0.1,
  "backgroundColor": "#FFF7D6",
  "textColor": "#111827",
  "themeName": "warm-low-glare"
}
```

Reset:

```http
POST /children/{childId}/display-settings/reset
```

## Output

```json
{
  "childId": "uuid",
  "fontFamily": "OpenDyslexic",
  "fontSize": 24,
  "lineHeight": 1.9,
  "letterSpacing": 0.1,
  "backgroundColor": "#FFF7D6",
  "textColor": "#111827",
  "themeName": "warm-low-glare",
  "settingsVersion": 4
}
```

## Data Model

`display_settings`

- `id uuid primary key`
- `child_id uuid not null unique references users(id)`
- `font_family text not null`
- `font_size integer not null`
- `line_height numeric(4,2) not null`
- `letter_spacing numeric(4,2) not null`
- `background_color text not null`
- `text_color text not null`
- `theme_name text null`
- `settings_version integer not null default 1`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

## Implementation

### 3.1 Access control

Allowed:

- Admin can read/update any child settings.
- Guardian can read/update settings for accepted child.
- Child can read own settings.
- Child can update/reset own settings.

### 3.2 Validation

- `fontSize`: min/max rõ ràng, ví dụ 14-40.
- `lineHeight`: ví dụ 1.0-3.0.
- `letterSpacing`: ví dụ 0-0.5.
- `backgroundColor`, `textColor`: validate hex color.
- `fontFamily`: tạm thời validate nonblank/max length; chưa dùng allowlist vì frontend chưa chốt font keys.

### 3.3 Preview

Preview realtime là trách nhiệm frontend, backend không cần endpoint riêng nếu không render server-side.

Backend chỉ cần:

- trả default settings.
- save settings.
- reset default.

### 3.4 Default settings

Tạo default trong code config hoặc DB seed:

```json
{
  "fontFamily": "system",
  "fontSize": 20,
  "lineHeight": 1.6,
  "letterSpacing": 0.04,
  "backgroundColor": "#FFFFFF",
  "textColor": "#111111",
  "themeName": "default"
}
```

## APIs

- `GET /children/{childId}/display-settings`
- `PUT /children/{childId}/display-settings`
- `POST /children/{childId}/display-settings/reset`

## Done Criteria

- Guardian accepted link có thể save/reset settings.
- Guardian không accepted link bị 403.
- Child lấy được settings của chính mình.
- Invalid color/font size bị reject.
- Reset trả default và increment version.
