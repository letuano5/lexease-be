# Phase 3 - Display Settings Process

## Implemented Features

- Flyway schema for display settings in `V3__init_display_settings.sql`.
- Per-child display settings storage.
- Default settings returned when a child has no saved row.
- Get, save, and reset APIs.
- Child can read/update/reset own settings.
- Guardian can read/update/reset settings only for an `ACCEPTED` child link.
- Admin can read/update/reset any child settings.
- Validation for font size, line height, letter spacing, hex colors, and basic `fontFamily`.
- Audit actions for save/reset.

## Data Model

Implemented table: `display_settings`.

- `id`: internal UUID primary key.
- `child_id`: unique child user id.
- `font_family`: font key/name from client. No allowlist yet.
- `font_size`: integer font size.
- `line_height`: numeric line height.
- `letter_spacing`: numeric letter spacing.
- `background_color`: hex color.
- `text_color`: hex color.
- `theme_name`: optional theme key/name.
- `settings_version`: monotonically increments on update/reset.
- `created_at`, `updated_at`: audit timestamps.

## Endpoint: `GET /children/{childId}/display-settings`

Returns saved settings, or defaults if none exist.

Input:

- Path `childId`: child UUID.

Output:

```json
{
  "childId": "uuid",
  "fontFamily": "system",
  "fontSize": 20,
  "lineHeight": 1.6,
  "letterSpacing": 0.04,
  "backgroundColor": "#FFFFFF",
  "textColor": "#111111",
  "themeName": "default",
  "settingsVersion": 1
}
```

Implementation:

- `DisplaySettingsController` delegates to `DisplaySettingsService.get`.
- Uses `PermissionService.canAccessChild`.
- If no DB row exists, returns `DisplaySettingsDefaults.standard()` without creating a row.

## Endpoint: `PUT /children/{childId}/display-settings`

Saves settings for a child.

Input:

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

Input fields:

- `fontFamily`: required, max 80 chars. No allowlist yet.
- `fontSize`: 14-40.
- `lineHeight`: 1.00-3.00.
- `letterSpacing`: 0.00-0.50.
- `backgroundColor`: required `#RRGGBB`.
- `textColor`: required `#RRGGBB`.
- `themeName`: optional, max 80 chars.

Output: same shape as get.

Implementation:

- Uses `PermissionService.canManageDisplaySettings`.
- First save creates row with `settingsVersion = 1`.
- Later saves increment `settingsVersion`.
- Hex colors are normalized to uppercase.
- Audit action: `DISPLAY_SETTINGS_SAVED`.

## Endpoint: `POST /children/{childId}/display-settings/reset`

Resets settings to defaults.

Input:

- Path `childId`: child UUID.

Output: same shape as get, with default values.

Implementation:

- Uses `PermissionService.canManageDisplaySettings`.
- If no row exists, creates default settings with `settingsVersion = 1`.
- If row exists, applies defaults and increments `settingsVersion`.
- Audit action: `DISPLAY_SETTINGS_RESET`.

## Decisions

- Child is allowed to update/reset own display settings.
- Font allowlist is not enforced yet because frontend has not finalized font keys.
- Migration history was squashed after Phase 3. Reset local/dev DB before applying the current `V1`-`V3` migration set.

## Verification

- `./gradlew compileJava` passes.
- `./gradlew test` passes.
- Added tests:
  - Child can save own settings.
  - Guardian without accepted link cannot save settings.
  - Accepted guardian reset returns defaults and increments existing version.
