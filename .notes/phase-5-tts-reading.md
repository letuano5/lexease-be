# Phase 5 - TTS, Word Timing, Reading Progress

Mục tiêu: bổ sung TTS audio và word-level timing cho truyện đã có trong database, để frontend phát audio và highlight từng từ khi đọc. Spring Boot không chạy model TTS/alignment trực tiếp; Spring gọi AI server, lưu audio vào object storage, lưu word timings vào database, và quản lý reading sessions/progress.

Luồng chốt là hybrid:

- Khi admin publish truyện, backend có thể tự tạo `tts_assets` status `PENDING` để worker xử lý nền.
- Vẫn phải có API admin để generate/refresh TTS chủ động cho một story có sẵn.
- Khi child bấm đọc, backend load active session cũ theo `storyId`; nếu chưa có thì tạo session mới.
- Resume theo `currentWordIndex`, không dùng block index.
- Public reading API dùng `/sessions`, không dùng `/reading-sessions`.

## Input

Admin generate/refresh TTS for an existing story:

```json
{
  "voice": "vieneu-voice-id",
  "refresh": false
}
```

Spring calls AI server:

```json
{
  "text": "Ngay xua co mot chu meo...",
  "voice": "vieneu-voice-id",
  "audioFormat": "wav",
  "language": "vi-VN"
}
```

AI server response:

```json
{
  "requestId": "uuid-or-trace-id",
  "voice": "vieneu-voice-id",
  "language": "vi-VN",
  "audio": {
    "mimeType": "audio/wav",
    "format": "wav",
    "durationMs": 124000,
    "contentBase64": "..."
  },
  "words": [
    {
      "index": 0,
      "text": "Ngay",
      "startMs": 120,
      "endMs": 410
    }
  ]
}
```

Notes:

- AI server owns VieNeu-TTS and any internal separation/alignment logic.
- Spring treats AI server as a black-box HTTP provider.
- Spring maps returned words back to `story_words.word_index`, `start_char`, and `end_char`.
- If audio is too large for base64, the same provider abstraction can support a temporary `audioUrl`, but the Spring responsibility remains the same: download/read audio, store it in application object storage, and persist timing rows.

Start or resume reading:

```json
{
  "storyId": "uuid",
  "voice": "vieneu-voice-id",
  "mode": "RESUME_OR_START"
}
```

Update progress:

```json
{
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

## Output

Admin generate/refresh TTS:

```json
{
  "assetId": "uuid",
  "storyId": "uuid",
  "storyVersion": 3,
  "voice": "vieneu-voice-id",
  "status": "READY",
  "audioUrl": "signed-url",
  "wordTimingCount": 240
}
```

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
    "assetId": "uuid",
    "voice": "vieneu-voice-id",
    "audioUrl": "signed-url",
    "wordTimings": [
      {
        "wordIndex": 0,
        "text": "Ngay",
        "startMs": 120,
        "endMs": 410,
        "startChar": 0,
        "endChar": 4
      }
    ]
  },
  "resumePosition": {
    "wordIndex": 0
  }
}
```

If TTS asset is not ready:

```json
{
  "tts": {
    "status": "PENDING",
    "assetId": "uuid",
    "voice": "vieneu-voice-id",
    "audioUrl": null,
    "wordTimings": []
  }
}
```

Frontend can show text-only or "audio preparing" while TTS is pending/failed.

## Data Model

`tts_assets`

- `id uuid primary key`
- `story_id uuid not null references stories(id)`
- `story_version integer not null`
- `provider text not null`
- `voice_id text not null`
- `audio_object_key text null`
- `audio_mime_type text null`
- `status text not null check in ('PENDING','PROCESSING','READY','FAILED','INVALIDATED')`
- `error_message text null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- unique `(story_id, story_version, voice_id)`

`tts_word_timings`

- `id uuid primary key`
- `tts_asset_id uuid not null references tts_assets(id) on delete cascade`
- `story_word_id uuid null references story_words(id)`
- `word_index integer not null`
- `text text not null`
- `start_ms integer not null`
- `end_ms integer not null`
- `start_char integer null`
- `end_char integer null`
- unique `(tts_asset_id, word_index)`

`reading_sessions`

- `id uuid primary key`
- `child_id uuid not null references users(id)`
- `story_id uuid not null references stories(id)`
- `story_version integer not null`
- `voice_id text not null`
- `status text not null check in ('IN_PROGRESS','COMPLETED','ABANDONED')`
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

### 5.1 TTS orchestration

Trong Spring Boot:

```text
TtsService
  enqueueAsset(storyId, voice)
  generateAssetNow(storyId, voice, refresh)
  refreshAsset(storyId, voice)
```

Spring responsibilities:

1. Load story by `storyId`; validate story exists and is eligible for TTS.
2. Create or reuse `tts_assets` by `(story_id, story_version, voice_id)`.
3. Set status `PENDING` for queued work or `PROCESSING` for synchronous admin refresh.
4. Call AI server with the story text and voice:

```json
{
  "text": "...",
  "voice": "vieneu-voice-id",
  "audioFormat": "wav",
  "language": "vi-VN"
}
```

5. Store returned audio via `ObjectStorageService`.
6. Map returned word timings to existing `story_words`.
7. Persist mapped timings in `tts_word_timings`.
8. Update asset `READY` or `FAILED`.

Do not let frontend send story text for TTS generation. The text source of truth is the story stored in the database.

### 5.2 Worker and admin refresh

Worker path:

- When story becomes `PUBLISHED`, create/enqueue `tts_assets` status `PENDING` for the default voice.
- Worker picks pending assets and calls the same `TtsService.generateAssetNow(...)`.
- This keeps published stories warm without waiting for the first child to open the reading screen.

Admin path:

- Admin can call generate/refresh API for a specific story and voice.
- If `refresh=false` and a `READY` asset exists for the current story version and voice, return existing asset.
- If `refresh=true`, invalidate or replace the current asset and regenerate audio/timings.
- Refresh is useful when voice selection changes, AI quality improves, a previous job failed, or audio/timing needs manual repair.

### 5.3 AI service provider contract

Endpoint đề xuất:

```http
POST /v1/tts/word-timings
Content-Type: application/json
Accept: application/json
```

Request schema:

```json
{
  "text": "story content from database",
  "voice": "vieneu-voice-id",
  "audioFormat": "wav",
  "language": "vi-VN"
}
```

Request fields:

- `text` string, required: full story content from Spring database.
- `voice` string, required: VieNeu-TTS voice id.
- `audioFormat` string, optional, default `wav`: allowed values `wav`, `mp3`.
- `language` string, optional, default `vi-VN`.

Success response schema:

```json
{
  "requestId": "uuid-or-trace-id",
  "voice": "vieneu-voice-id",
  "language": "vi-VN",
  "audio": {
    "mimeType": "audio/wav",
    "format": "wav",
    "sampleRateHz": 22050,
    "durationMs": 124000,
    "contentBase64": "..."
  },
  "words": [
    {
      "index": 0,
      "text": "Ngay",
      "normalizedText": "ngay",
      "startMs": 120,
      "endMs": 410,
      "confidence": 0.98
    }
  ],
  "metadata": {
    "model": "vieneu-tts",
    "alignmentMethod": "word-separation",
    "createdAt": "2026-05-26T10:30:00Z"
  }
}
```

Success response fields:

- `requestId` string, optional but recommended: trace id for logs/debugging.
- `voice`: actual voice id used.
- `language`: actual language used.
- `audio.mimeType`: required, for example `audio/wav` or `audio/mpeg`.
- `audio.format`: required, allowed values `wav`, `mp3`.
- `audio.sampleRateHz`: optional.
- `audio.durationMs`: optional but recommended.
- `audio.contentBase64`: required for MVP. Later can be replaced by `audio.url` if files become too large.
- `words`: required, non-empty ordered word timing list.
- `words[].index`: required, zero-based index in AI output order.
- `words[].text`: required, the word/token text aligned by AI.
- `words[].normalizedText`: optional.
- `words[].startMs`: required, integer, inclusive start time from audio beginning.
- `words[].endMs`: required, integer, exclusive end time from audio beginning.
- `words[].confidence`: optional, number from `0` to `1`.
- `metadata`: optional provider diagnostics.

Error response schema:

```json
{
  "requestId": "uuid-or-trace-id",
  "error": {
    "code": "TTS_GENERATION_FAILED",
    "message": "Could not synthesize audio",
    "retryable": true,
    "details": {
      "provider": "vieneu-tts"
    }
  }
}
```

Error rules:

- Use `400` for invalid request, unsupported voice, empty text, or unsupported audio format.
- Use `422` when text is valid JSON input but cannot be tokenized/aligned.
- Use `500` for provider/model failures.
- Use `503` for temporary model/server overload.
- Spring stores `error.message` into `tts_assets.error_message` and marks the asset `FAILED`.

Validation rules Spring expects:

- Returned `words` must be sorted by `index`.
- `startMs >= 0`, `endMs > startMs`.
- Word timings should be monotonic: each word starts at or after the previous word's start.
- AI output word count should be close enough to `story_words` count for order-based mapping. Large mismatch should fail the asset instead of producing misleading highlight data.

Spring should not depend on MFA/TextGrid details. If the AI server internally uses VieNeu-TTS, MFA, or another alignment method, that stays behind the AI service boundary.

### 5.4 Word timing mapping

Backend canonical timing source is `tts_word_timings` in DB.

Mapping rules:

- Match AI returned words to `story_words` in order.
- Persist `word_index`, `text`, `start_ms`, `end_ms`, `start_char`, and `end_char`.
- If exact text differs because of normalization, punctuation, or Vietnamese tokenization, keep matching order-based but log/report mismatch.
- Add tests for punctuation, accents, and repeated words because these are the likely failure cases.

Optional export formats like WebVTT/SRT can be added later, but DB rows are the source of truth for MVP.

### 5.5 Reading session

Start/resume:

- Check current user role `CHILD`.
- Check story is published and not blocked.
- Use `/sessions`.
- If `mode = RESUME_OR_START`, return the active `IN_PROGRESS` session for `(child_id, story_id, voice_id)` if it exists; otherwise create a new session.
- If `mode = START_FROM_BEGINNING`, create a new session with `current_word_index = 0`.
- Return TTS status, signed audio URL, and word timings from DB if asset is ready.

Progress:

- Frontend sends checkpoint every 5-10 seconds, on pause, on exit, and on complete.
- Backend stores `current_word_index` and `elapsed_ms`.
- Backend validates monotonic-ish progress but should tolerate retries.
- Store `reading_events` for analytics:
  - `START`
  - `PAUSE`
  - `RESUME`
  - `WORD_SHOWN`
  - `TTS_HELP`
  - `COMPLETE`

### 5.6 Signed URLs

Use `ObjectStorageService` abstraction:

- `putObject`
- `getSignedReadUrl`
- `getSignedWriteUrl`

MVP can store local files and return local dev URLs, but API contract should look like signed URL.

## APIs

- `POST /admin/stories/{storyId}/tts-assets`: admin only; generate or refresh TTS for an existing story.
- `GET /admin/stories/{storyId}/tts-assets`: admin only; inspect available assets/status by voice.
- `POST /sessions`: child start/resume reading.
- `GET /sessions/active?storyId=uuid&voice=vieneu-voice-id`: child get active session.
- `GET /sessions/{id}`: child get session detail.
- `PATCH /sessions/{id}/progress`: child update progress/events.
- `POST /sessions/{id}/complete`: child complete session.
- internal: `POST /internal/tts/jobs/{id}/callback` only if the AI service becomes async.

## Done Criteria

- Publishing a story can enqueue a default TTS asset job.
- Admin can generate/refresh TTS for an existing story and voice.
- Spring calls AI server with story `text`, `voice`, `audioFormat`, and `language`.
- Spring stores returned audio in object storage.
- Spring persists word timings in `tts_word_timings`.
- Timing rows map to `story_words.word_index`.
- Child can start/resume via `/sessions` and resume by `currentWordIndex`.
- Session response returns TTS status, signed audio URL, and DB-backed word timings when ready.
- Progress checkpoints persist.
- Blocked story cannot be started by child.
- TTS failure is visible and recoverable by admin refresh or retry job.
