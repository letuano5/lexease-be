# Phase 5 - TTS And Reading Process

## Implemented Features

- Flyway schema for TTS assets, word timings, reading sessions, and reading events in `V5__init_reading_tts.sql`.
- Admin TTS asset generation and listing for published stories.
- Async TTS provider integration:
  - Submit job to external TTS server.
  - Receive signed provider callback.
  - Store returned audio through object storage.
  - Persist word-level timings mapped to `story_words`.
- Sync TTS provider mode behind config for local/provider variants that return audio immediately.
- Automatic default TTS generation attempt when an admin creates or updates a story as `PUBLISHED`.
- Child reading sessions using `/sessions`.
- Resume by `currentWordIndex`, not by block index.
- Reading progress updates with optional reading events.
- Reading completion.
- Local object storage implementation for generated audio and signed read URLs.
- Public signed local storage read endpoint.
- TTS and reading config are type-bound with `@ConfigurationProperties`.

Important implementation note:

- There is no scheduled polling worker yet. Current async flow submits work to the TTS provider immediately and depends on provider callback to mark the asset `READY` or `FAILED`.

## Data Model

Implemented in `V5__init_reading_tts.sql`.

`tts_assets`

- `id`: UUID primary key. Also used as the internal provider `requestId`.
- `story_id`: story UUID.
- `story_version`: story version at generation time.
- `provider`: configured provider name.
- `provider_request_id`: request id sent to provider.
- `provider_job_id`: optional provider job id returned by async submit.
- `voice_id`: requested or default voice.
- `audio_object_key`: object storage key when audio is ready.
- `audio_mime_type`: stored audio MIME type.
- `audio_duration_ms`: optional duration from provider.
- `audio_sample_rate_hz`: optional sample rate from provider.
- `status`: `PENDING`, `PROCESSING`, `READY`, `FAILED`, or `INVALIDATED`.
- `error_message`: failure reason from provider or backend validation.
- `created_at`, `updated_at`: audit timestamps.
- Unique index: `(story_id, story_version, voice_id)`.

`tts_word_timings`

- `id`: UUID primary key.
- `tts_asset_id`: TTS asset UUID, cascades on asset delete.
- `story_word_id`: mapped `story_words` row, nullable.
- `word_index`: zero-based word index.
- `text`: provider word text.
- `start_ms`, `end_ms`: audio timing range.
- `start_char`, `end_char`: copied from mapped `story_words`.
- Unique index: `(tts_asset_id, word_index)`.

`reading_sessions`

- `id`: UUID primary key.
- `child_id`: child user UUID.
- `story_id`: story UUID.
- `story_version`: story version when session was created.
- `voice_id`: voice selected for the session.
- `status`: `IN_PROGRESS`, `COMPLETED`, or `ABANDONED`.
- `current_word_index`: resume word index.
- `elapsed_ms`: client-reported elapsed reading/audio time.
- `started_at`, `last_active_at`, `completed_at`: timestamps.

`reading_events`

- `id`: UUID primary key.
- `session_id`: reading session UUID, cascades on session delete.
- `event_type`: `START`, `PAUSE`, `RESUME`, `WORD_SHOWN`, `TTS_HELP`, or `COMPLETE`.
- `word`: optional word text.
- `word_index`: optional word index.
- `timestamp_ms`: optional client-side event timestamp.
- `metadata`: JSONB payload, default `{}`.
- `created_at`: server timestamp.

## Config

TTS config under `lexease.tts`:

- `TTS_BASE_URL`: external TTS provider base URL. Required when provider calls are made.
- `TTS_CALLBACK_BASE_URL`: public backend base URL used to build callback URL.
- `TTS_CALLBACK_SECRET`: HMAC secret for provider callbacks.
- `TTS_MODE`: `ASYNC` by default; `SYNC` calls provider synchronously.
- `TTS_PROVIDER`: provider label. Default: `vieneu-tts`.
- `TTS_DEFAULT_VOICE`: default voice. Default: `Binh`.
- `TTS_AUDIO_FORMAT`: requested audio format. Default: `wav`.
- `TTS_LANGUAGE`: requested language. Default: `vi-VN`.
- `TTS_CALLBACK_TIMESTAMP_TOLERANCE`: callback timestamp tolerance. Default: `5m`.
- `TTS_MAX_WORD_COUNT_MISMATCH_RATIO`: allowed provider/story word-count mismatch ratio. Default: `0.10`.
- `TTS_AUTO_GENERATE_ON_PUBLISH`: whether published stories auto-submit default TTS. Default: `true`.

Object storage config under `lexease.object-storage`:

- `OBJECT_STORAGE_MODE`: storage mode placeholder. Current implementation is local storage.
- `OBJECT_STORAGE_LOCAL_ROOT`: local file root. Default: `build/lexease-storage`.
- `OBJECT_STORAGE_PUBLIC_BASE_URL`: prefix for signed read URLs.
- `OBJECT_STORAGE_SIGNING_SECRET`: HMAC secret for signed local read URLs.
- `OBJECT_STORAGE_READ_URL_TTL`: signed URL TTL. Default: `15m`.

## Endpoint: `POST /admin/stories/{storyId}/tts-assets`

Generates or refreshes a TTS asset for a published story. Admin only.

Input:

```json
{
  "voice": "Binh",
  "refresh": false
}
```

Input fields:

- `voice`: optional, max 100 chars. Blank or missing uses configured default voice.
- `refresh`: optional boolean. `true` regenerates even if a `READY` asset already exists.

Output:

```json
{
  "assetId": "uuid",
  "storyId": "uuid",
  "storyVersion": 3,
  "voice": "Binh",
  "status": "PROCESSING",
  "audioUrl": null,
  "wordTimingCount": 0,
  "providerJobId": "external-job-id",
  "errorMessage": null
}
```

Implementation:

- `AdminTtsAssetController` delegates to `TtsService.generateAsset`.
- Story must exist and be `PUBLISHED`.
- Asset identity is `(storyId, storyVersion, voice)`.
- If `refresh=false` and a `READY` asset exists, returns it with HTTP `200`.
- Otherwise, clears existing timings, marks asset `PROCESSING`, and calls provider.
- In `ASYNC` mode, submits `POST /v1/tts/jobs` to provider and returns HTTP `202 Accepted`.
- In `SYNC` mode, calls provider `POST /v1/tts/word-timings`, stores the result immediately, and returns HTTP `200`.
- Provider failures mark the asset `FAILED`.

Provider async submit body:

```json
{
  "requestId": "asset-uuid",
  "callbackUrl": "https://api.example.com/internal/tts/jobs/asset-uuid/callback",
  "text": "Ngay xua co mot chu meo...",
  "voice": "Binh",
  "audioFormat": "wav",
  "language": "vi-VN"
}
```

## Endpoint: `GET /admin/stories/{storyId}/tts-assets`

Lists current-version TTS assets for a story. Admin only.

Input:

- Path `storyId`: story UUID.

Output:

```json
[
  {
    "assetId": "uuid",
    "storyId": "uuid",
    "storyVersion": 3,
    "voice": "Binh",
    "status": "READY",
    "audioUrl": "/storage/local?key=tts%2F...",
    "wordTimingCount": 240,
    "providerJobId": "external-job-id",
    "errorMessage": null
  }
]
```

Implementation:

- Loads the story and returns assets for the story's current version ordered by latest update.
- `audioUrl` is present only when the asset is `READY` and has an audio object key.

## Endpoint: `POST /internal/tts/jobs/{requestId}/callback`

Receives async TTS provider completion callbacks.

Input headers:

- `X-Lexease-Timestamp`: epoch seconds.
- `X-Lexease-Signature`: `sha256=<hex-hmac>`.

The HMAC payload is:

```text
<timestamp>.<raw-json-body>
```

Input body:

```json
{
  "jobId": "external-job-id",
  "status": "READY",
  "requestId": "asset-uuid",
  "voice": "Binh",
  "language": "vi-VN",
  "audio": {
    "mimeType": "audio/wav",
    "format": "wav",
    "sampleRateHz": 24000,
    "durationMs": 124000,
    "contentBase64": "..."
  },
  "words": [
    {
      "index": 0,
      "text": "Ngay",
      "normalizedText": "ngay",
      "startMs": 120,
      "endMs": 410
    }
  ],
  "error": null
}
```

Output: HTTP `204 No Content`.

Implementation:

- Endpoint is unauthenticated at JWT level but protected by HMAC signature and timestamp tolerance.
- Path `requestId` must match body `requestId`.
- The TTS asset is loaded by UUID.
- `FAILED` callback marks the asset `FAILED` with provider error.
- `READY` callback validates audio and word timings, writes audio to object storage, replaces timing rows, and marks the asset `READY`.
- Unsupported callback status returns `INVALID_TTS_CALLBACK`.

Validation before storing a provider result:

- Audio content, MIME type, and format are required.
- Word timings are required.
- Provider word count can be shorter than story words only within configured mismatch ratio.
- Provider word count cannot exceed stored story word count.
- Word indexes must be zero-based and contiguous.
- Timings must be positive and sorted by `startMs`.

## Endpoint: `POST /sessions`

Starts or resumes a child reading session.

Input:

```json
{
  "storyId": "uuid",
  "voice": "Binh",
  "mode": "RESUME_OR_START"
}
```

Input fields:

- `storyId`: required story UUID.
- `voice`: optional, max 100 chars. Blank or missing uses configured default voice.
- `mode`: optional. Defaults to `RESUME_OR_START`. `START_FROM_BEGINNING` creates a new session from word index `0`.

Output:

```json
{
  "sessionId": "uuid",
  "status": "IN_PROGRESS",
  "story": {
    "id": "uuid",
    "title": "Chu meo di hoc",
    "content": "Ngay xua co mot chu meo..."
  },
  "tts": {
    "status": "READY",
    "assetId": "uuid",
    "voice": "Binh",
    "audioUrl": "/storage/local?key=tts%2F...",
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
    "wordIndex": 12
  },
  "elapsedMs": 94000
}
```

Implementation:

- Only `CHILD` users can use reading sessions.
- Story must be `PUBLISHED`.
- Child cannot read a story hidden by an active block from an accepted guardian.
- Ensures a TTS asset exists/has been queued for the selected voice.
- `RESUME_OR_START` reuses the latest `IN_PROGRESS` session for `(child, story, voice)`, or creates one.
- A `START` reading event is recorded.
- Response includes TTS timings only if the asset is `READY`; otherwise `wordTimings` is empty and `audioUrl` is `null`.

## Endpoint: `GET /sessions/active`

Returns the latest active session for a child/story/voice.

Input:

```http
GET /sessions/active?storyId=uuid&voice=Binh
```

Input fields:

- `storyId`: required story UUID.
- `voice`: optional. Blank or missing uses configured default voice.

Output: same shape as `POST /sessions`.

Implementation:

- Only child users are allowed.
- Looks up latest `IN_PROGRESS` session by `(child, story, voice)`.
- Revalidates story visibility before returning the session.

## Endpoint: `GET /sessions/{id}`

Returns a reading session by id.

Input:

- Path `id`: session UUID.

Output: same shape as `POST /sessions`.

Implementation:

- Only the child who owns the session can read it.
- Revalidates story visibility before returning the session.

## Endpoint: `PATCH /sessions/{id}/progress`

Updates reading progress and stores optional reading events.

Input:

```json
{
  "currentWordIndex": 125,
  "elapsedMs": 92300,
  "events": [
    {
      "type": "TTS_HELP",
      "word": "kho",
      "wordIndex": 125,
      "timestampMs": 92300,
      "metadata": {
        "source": "player"
      }
    }
  ]
}
```

Input fields:

- `currentWordIndex`: required, minimum `0`.
- `elapsedMs`: required, minimum `0`.
- `events`: optional array.
- `events[].type`: required event type.
- `events[].word`: optional, max 200 chars.
- `events[].wordIndex`: optional word index.
- `events[].timestampMs`: optional client timestamp.
- `events[].metadata`: optional JSON object.

Output: same shape as `POST /sessions`.

Implementation:

- Only the child who owns the session can update it.
- Session must be `IN_PROGRESS`.
- Progress is monotonic: backend keeps the max of existing and incoming `currentWordIndex`/`elapsedMs`.
- Optional events are inserted into `reading_events`.

## Endpoint: `POST /sessions/{id}/complete`

Marks a reading session as completed.

Input:

- Path `id`: session UUID.

Output: same shape as `POST /sessions`, with `status` as `COMPLETED`.

Implementation:

- Only the child who owns the session can complete it.
- Sets `completed_at`, updates `last_active_at`, and records a `COMPLETE` event.

## Endpoint: `GET /storage/local`

Reads a locally stored object through a signed URL.

Input:

```http
GET /storage/local?key=tts%2F...&expires=1710000000&signature=hex
```

Input fields:

- `key`: object storage key.
- `expires`: expiry epoch seconds.
- `signature`: HMAC signature for `<key>.<expires>`.

Output:

- Raw object bytes with stored content type.

Implementation:

- Endpoint is public because access is controlled by signed URL.
- Expired or invalid signatures return `FORBIDDEN`.
- Local object path is normalized under configured storage root to prevent path traversal.

## Decisions

- TTS generation never trusts story text from the client. `TtsService` always loads text from the persisted story.
- Reading APIs are intentionally child-only. Guardian/admin reading views can be added later as separate reporting endpoints.
- TTS asset identity includes story version, so story edits naturally create a new asset key.
- Timing rows map provider indexes onto existing `story_words`; this reuses the Phase 2 tokenizer offsets for frontend highlighting.
- Audio is stored through `ObjectStorageService`; current implementation is local storage, but the service boundary is ready for S3/Supabase-style storage.
- `PENDING`, `PROCESSING`, `FAILED`, and `READY` are returned to frontend so it can render text-only, preparing, failed, or audio-ready states.

## Verification

- Added focused tests:
  - `TtsServiceTests`
  - `TtsCallbackVerifierTests`
  - `ReadingServiceTests`
- Existing app context and domain tests remain in place.
