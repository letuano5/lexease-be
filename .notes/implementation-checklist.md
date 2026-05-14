# Implementation Checklist

Use this as the single file to track done/not done.

## Phase 0 - Foundation

- [x] Create Spring Boot project with Java 21.
- [x] Add web, validation, security, JPA, Flyway, PostgreSQL dependencies.
- [x] Add local/test profiles.
- [x] Add Docker Compose PostgreSQL.
- [x] Add Flyway baseline migration.
- [x] Add global error response.
- [x] Add health endpoint.
- [x] Add initial test setup.

## Phase 1 - Auth And Authorization

- [x] Create `users` migration.
- [x] Create `refresh_tokens` migration.
- [x] Create `guardian_child_links` migration.
- [x] Create `audit_logs` migration.
- [x] Implement password hashing.
- [x] Implement register.
- [x] Implement login.
- [x] Implement JWT access token signing/verifying.
- [x] Implement opaque refresh token hashing/storage.
- [x] Implement refresh token rotation.
- [x] Implement logout/revoke refresh token.
- [x] Implement `/me`.
- [x] Add SQL promote script for first admin.
- [x] Implement admin user list/status update.
- [x] Implement guardian-child link request.
- [x] Implement link accept/reject/revoke.
- [x] Implement `PermissionService`.
- [x] Add permission tests.

## Phase 2 - Story Management

- [x] Create `stories` migration.
- [x] Create `genres` migration.
- [x] Create `authors` migration.
- [x] Create `story_genres` migration.
- [x] Create `story_authors` migration.
- [x] Create `story_words` migration.
- [x] Create `story_access_blocks` migration.
- [x] Implement story text normalization.
- [x] Implement story word splitting.
- [x] Implement admin create story.
- [x] Implement admin update story with version increment.
- [x] Implement archive story.
- [x] Implement search/filter by keyword.
- [x] Implement filter by genre IDs.
- [x] Implement filter by author IDs.
- [x] Implement metadata list APIs for genres/authors.
- [x] Implement admin genre create/update/soft delete.
- [x] Implement admin author create/update/soft delete.
- [x] Implement child visibility rule.
- [x] Implement guardian block/unblock story.
- [x] Add tests for blocked story exclusion.

## Phase 3 - Display Settings

- [x] Create `display_settings` migration.
- [x] Define default settings.
- [x] Implement get settings.
- [x] Implement save settings.
- [x] Implement reset settings.
- [x] Validate font/size/color/spacing.
- [x] Add permission tests for guardian-child relation.

## Phase 4 - Notifications

- [ ] Create `device_tokens` migration.
- [ ] Create `reminder_schedules` migration.
- [ ] Create `notification_events` migration.
- [ ] Implement device token register/deactivate.
- [ ] Implement create/update/delete reminder.
- [ ] Implement next-run calculation by timezone.
- [ ] Implement scheduler scan due reminders.
- [ ] Implement push sender interface.
- [ ] Implement FCM/APNs provider choice.
- [ ] Implement deep link payload.
- [ ] Implement notification status tracking.
- [ ] Implement ignored/late classification job.

## Phase 5 - TTS And Reading

- [ ] Create `tts_assets` migration.
- [ ] Create `reading_sessions` migration.
- [ ] Create `reading_events` migration.
- [ ] Implement object storage abstraction.
- [ ] Implement TTS asset job creation when story published.
- [ ] Scaffold Python AI service.
- [ ] Integrate VieNeu TTS in Python service.
- [ ] Install/configure MFA Vietnamese acoustic model.
- [ ] Parse MFA TextGrid output.
- [ ] Map aligned words to `story_words`.
- [ ] Store audio object.
- [ ] Store word timing JSON object.
- [ ] Implement start reading session.
- [ ] Implement active/resume session.
- [ ] Implement progress checkpoint.
- [ ] Implement complete session.
- [ ] Add tests for blocked story cannot start reading.

## Phase 6 - Gemini Scoring

- [ ] Create `recordings` migration.
- [ ] Create `ai_model_configs` migration.
- [ ] Create `ai_evaluations` migration.
- [ ] Implement upload session.
- [ ] Implement complete upload.
- [ ] Implement evaluation job creation.
- [ ] Implement Gemini client.
- [ ] Make Gemini model configurable.
- [ ] Define prompt version and JSON schema.
- [ ] Validate Gemini JSON response.
- [ ] Persist transcript/summary/scores/errors/difficult words.
- [ ] Implement retry failed evaluation.
- [ ] Implement progress summary.
- [ ] Implement progress timeseries.
- [ ] Implement difficult words endpoint.
- [ ] Implement session detail endpoint.

## Cross-Cutting

- [ ] Add audit logs for sensitive actions.
- [ ] Add rate limit for login/register.
- [ ] Add signed URL expiry config.
- [ ] Add retention config for recordings.
- [ ] Add OpenAPI docs or generated API contract.
- [ ] Add seed data for local dev.
