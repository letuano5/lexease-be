# LexEase Backend Implementation Plan

Nguồn yêu cầu chính: `.notes/drafts.md` và các chốt mới từ bạn:

- Xuất phát từ repo/DB trống.
- Stack: Java 21, Spring Boot bản stable mới nhất tại thời điểm bootstrap, Hibernate/JPA, Flyway, PostgreSQL.
- Auth tự cài: tự hash password, tự sign token. Không làm forgot password và verify email trong MVP.
- Story `genre` và `author` là mảng.
- TTS dùng VieNeu + MFA Vietnamese acoustic model để forced-align audio với text.
- Gemini model phải cấu hình được, không hard-code.

## File Map

- `phase-0-foundation.md`: bootstrap project, DB, migration, local dev.
- `phase-1-auth.md`: auth, token, role, relation-based authorization.
- `phase-2-stories.md`: quản lý truyện, search/filter, block story.
- `phase-3-display-settings.md`: tuỳ chỉnh hiển thị cho trẻ khó đọc.
- `phase-4-notifications.md`: push, reminder, deep link, tracking trạng thái.
- `phase-5-tts-reading.md`: VieNeu TTS, MFA aligner, reading session/progress, subtitle timing.
- `phase-6-gemini-scoring.md`: recording upload, Gemini scoring, progress analytics.
- `implementation-checklist.md`: checklist theo phase để tick đã làm/chưa làm.
- `open-questions.md`: các điểm cần bạn chốt trước khi implement chi tiết.
- `process/`: tài liệu implementation thực tế theo từng phase sau khi code xong.

## Process Documentation Rule

- Các file `phase-*.md` ở ngoài là plan/thuật toán/yêu cầu dự kiến, không dùng để ghi chi tiết implementation thực tế.
- Sau khi implement xong phase nào, tạo hoặc cập nhật file riêng trong `.notes/process/` với tên phase tương ứng.
- Mỗi file process phải có:
  - Các tính năng và endpoint đã implement.
  - Input/output cho từng endpoint, kèm giải thích các trường.
  - Giải thích ngắn cách implement từng tính năng.
  - Verification đã chạy, ví dụ `./gradlew test`, smoke local, migration.
- Nếu implementation khác plan ban đầu do quyết định mới, ghi khác biệt đó trong file process tương ứng.

## Phase Order

1. Foundation: tạo Spring Boot project, Docker Compose dev, Flyway baseline, CI/test conventions.
2. Auth: đây là trục quyền truy cập cho toàn bộ app, phải xong trước story/progress.
3. Story management: tạo dữ liệu truyện, search/filter, block theo child.
4. Display settings: độc lập, ít phụ thuộc, làm sớm để frontend có API.
5. Notification: cần auth + guardian-child relation + device token.
6. TTS/reading: phụ thuộc story và child access, có nhiều tích hợp ngoài Java.
7. Gemini scoring: phụ thuộc recording/reading session và tạo ra analytics.

## High-Level Architecture

```text
React Native App
  -> Spring Boot API
  -> PostgreSQL
  -> Object storage: local filesystem in dev, MinIO/S3-compatible later
  -> Python AI service for VieNeu TTS + MFA alignment
  -> Gemini API for reading evaluation
  -> FCM/APNs delivery for mobile push
```

Spring Boot nên tổ chức theo domain:

```text
com.lexease.auth
com.lexease.users
com.lexease.guardians
com.lexease.authors
com.lexease.genres
com.lexease.stories
com.lexease.display
com.lexease.notifications
com.lexease.reading
com.lexease.recordings
com.lexease.ai
com.lexease.shared
```

Trong từng domain, tổ chức theo pattern đang dùng ở backend cũ `ticket-rush-be`:

```text
com.lexease.<domain>
com.lexease.<domain>.dtos.req
com.lexease.<domain>.dtos.res
```

- Entity, enum, repository, service, controller và helper nội bộ ở domain root.
- Request DTO đặt trong `dtos.req`.
- Response DTO đặt trong `dtos.res`.
- Không để request/response DTO lẫn với entity/service ở domain root.

## Shared Rules

- Không expose JPA entity trực tiếp qua API; dùng request/response DTO.
- Mọi schema tạo bằng Flyway, không để Hibernate tự mutate schema trong production.
- Mọi API ghi dữ liệu cần audit tối thiểu: `created_at`, `updated_at`, actor nếu có ý nghĩa.
- Dùng UUID làm public id.
- Dùng `timestamptz` cho thời gian.
- File audio/timing dùng signed URL, không public object URL.
- Permission không chỉ dựa vào role. Guardian muốn thao tác child phải có link `ACCEPTED`.
- Không dùng magic value cho domain constants. Các nhóm giá trị cố định phải có enum hoặc constant type rõ ràng, ví dụ role/status, error code, audit action, target type, provider/status/task type.
- Không truyền string literal cho error code hoặc audit action trong service/controller; dùng `ErrorCode`, `AuditAction`, `AuditTargetType` hoặc enum tương ứng của domain.

## MVP Definition

MVP được coi là xong khi:

- User có thể register/login/refresh/logout.
- Admin có thể quản lý user cơ bản và CRUD story.
- Guardian có thể link child, block story, cài display settings, tạo reminder.
- Child có thể list/search story hợp lệ, bắt đầu/resume reading session.
- Story published có TTS audio + word timings từ VieNeu + MFA hoặc trạng thái pending rõ ràng.
- Child upload recording, backend gọi Gemini async và lưu kết quả.
- Guardian xem được progress summary, difficult words và session detail.
