# Phase 5 - TTS, MFA Alignment, Reading Progress

Mục tiêu: dùng VieNeu TTS để sinh audio tiếng Việt, dùng MFA Vietnamese acoustic model để align audio với transcript, lưu word timings để frontend highlight chữ theo voice như subtitle. Đồng thời lưu tiến độ đọc và events.

## Input

Start reading:

```json
{
  "storyId": "uuid",
  "mode": "START_FROM_BEGINNING"
}
```

Resume active session:

```http
GET /reading-sessions/active?storyId=uuid
```

Update progress:

```json
{
  "sessionId": "uuid",
  "currentBlockIndex": 6,
  "currentWordIndex": 12,
  "elapsedMs": 94000,
  "events": [
    {
      "type": "TTS_HELP",
      "word": "kho",
      "wordIndex": 125,
      "timestampMs": 92300
    }
  ]
}
```

Internal TTS job:

```json
{
  "storyId": "uuid",
  "storyVersion": 3,
  "voiceId": "vieneu-default",
  "text": "Ngay xua co mot chu meo..."
}
```

## Output

Start/resume reading:

```json
{
  "sessionId": "uuid",
  "story": {
    "id": "uuid",
    "title": "Chu meo di hoc",
    "content": "..."
  },
  "tts": {
    "status": "READY",
    "audioUrl": "signed-url",
    "timingUrl": "signed-url",
    "timingFormat": "WORD_TIMINGS_JSON"
  },
  "resumePosition": {
    "blockIndex": 0,
    "wordIndex": 0
  }
}
```

Word timing JSON:

```json
{
  "storyId": "uuid",
  "storyVersion": 3,
  "voiceId": "vieneu-default",
  "words": [
    {
      "wordIndex": 0,
      "text": "Ngay",
      "startMs": 120,
      "endMs": 410,
      "startChar": 0,
      "endChar": 4
    }
  ]
}
```

## Data Model

`tts_assets`

- `id uuid primary key`
- `story_id uuid not null references stories(id)`
- `story_version integer not null`
- `provider text not null`
- `voice_id text not null`
- `audio_object_key text null`
- `timing_object_key text null`
- `timing_format text null check in ('WORD_TIMINGS_JSON','WEBVTT','SRT')`
- `status text not null check in ('PENDING','PROCESSING','READY','FAILED','INVALIDATED')`
- `error_message text null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- unique `(story_id, story_version, voice_id)`

`reading_sessions`

- `id uuid primary key`
- `child_id uuid not null references users(id)`
- `story_id uuid not null references stories(id)`
- `story_version integer not null`
- `status text not null check in ('IN_PROGRESS','COMPLETED','ABANDONED')`
- `current_block_index integer not null default 0`
- `current_word_index integer not null default 0`
- `elapsed_ms bigint not null default 0`
- `started_at timestamptz not null`
- `last_active_at timestamptz not null`
- `completed_at timestamptz null`

`reading_events`

- `id uuid primary key`
- `session_id uuid not null references reading_sessions(id)`
- `event_type text not null`
- `word text null`
- `word_index integer null`
- `timestamp_ms bigint null`
- `metadata jsonb not null default '{}'`
- `created_at timestamptz not null`

## Implementation

### 5.1 TTS provider interface

Trong Spring Boot:

```text
TtsService
  generateAsset(storyId, storyVersion, voiceId)
```

Không nhúng VieNeu/MFA trực tiếp vào Spring process. Tạo Python AI service riêng để dễ quản lý model/native dependency.

Spring orchestration:

1. Khi story `PUBLISHED`, tạo `tts_assets` status `PENDING`.
2. Worker gọi Python AI service.
3. Python trả audio + timing hoặc object keys.
4. Spring update asset `READY` hoặc `FAILED`.

### 5.2 Python AI service pipeline

Pipeline chính:

```text
story text
-> normalize transcript for TTS/alignment
-> VieNeu-TTS generates wav
-> MFA Vietnamese acoustic model aligns wav + transcript
-> MFA outputs TextGrid
-> parse TextGrid
-> map aligned tokens back to story_words
-> write word_timings.json
-> store audio + timing
```

MFA model link từ draft/user: Vietnamese MFA acoustic model v2.0.0.

Important:

- VieNeu chưa được giả định là có SRT/word timestamp built-in.
- MFA là bước forced alignment riêng: audio + transcript -> timing.
- Nếu MFA lệch token vì dấu/cách tách từ tiếng Việt, cần lớp normalization/mapping có test.

### 5.3 Timing format

Backend lưu canonical `WORD_TIMINGS_JSON`.

Có thể export thêm `WEBVTT` nếu frontend muốn subtitle native. Nhưng source of truth nên là JSON vì cần `wordIndex`, `startChar`, `endChar`.

### 5.4 Reading session

Start:

- Check current user role `CHILD`.
- Check story published and not blocked.
- If mode `START_FROM_BEGINNING`, create new session.
- If active session exists and mode resume, return existing.
- Return TTS status and signed URLs if asset ready.

Progress:

- Frontend sends checkpoint every 5-10 seconds, pause, exit, complete.
- Backend validates monotonic-ish progress but should tolerate retries.
- Store `reading_events` for analytics:
  - `START`
  - `PAUSE`
  - `RESUME`
  - `WORD_SHOWN`
  - `TTS_HELP`
  - `COMPLETE`

### 5.5 Signed URLs

Use `ObjectStorageService` abstraction:

- `putObject`
- `getSignedReadUrl`
- `getSignedWriteUrl`

MVP can store local files and return local dev URLs, but API contract should look like signed URL.

### 5.6 Failure handling

If TTS asset failed/pending:

```json
{
  "tts": {
    "status": "PENDING",
    "audioUrl": null,
    "timingUrl": null,
    "timingFormat": null
  }
}
```

Frontend can show text-only or “audio preparing”.

## APIs

- `POST /reading-sessions`
- `GET /reading-sessions/active`
- `PATCH /reading-sessions/{id}/progress`
- `POST /reading-sessions/{id}/complete`
- `GET /tts-assets/{storyId}`
- internal: `POST /internal/tts/jobs/{id}/callback` if Python service async

## Done Criteria

- Publishing story creates TTS asset job.
- VieNeu + MFA pipeline creates audio and word timings for a sample Vietnamese story.
- Timing JSON maps to `story_words.word_index`.
- Child can start/resume session and receive TTS URLs.
- Progress checkpoints persist.
- Blocked story cannot be started by child.
- TTS failure is visible and recoverable by retry job.

