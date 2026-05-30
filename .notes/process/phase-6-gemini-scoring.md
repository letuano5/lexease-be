# Phase 6 - Gemini Scoring And Progress Analytics Process

## Implemented Features

- Flyway schema for `recordings` and `ai_evaluations` in `V6__init_recording_scoring.sql`, with `V7__recording_single_audio.sql` switching recordings to one audio file each.
- Child recording creation for an existing reading session using one audio payload plus the expected text segment being read.
- Local object storage for the recorded audio file and signed playback URL in responses.
- Recording CRUD:
  - Child owner can create/read/update/delete own recordings.
  - Accepted guardian/admin can read child recordings.
  - Accepted guardian/admin and child owner can soft-delete recordings.
- Evaluation lifecycle:
  - Spring creates an `ai_evaluations` row per recording.
  - Spring calls the external AI server, not Gemini directly.
  - Async mode submits `/v1/reading-evaluations/jobs` and waits for signed callback.
  - Sync mode posts `/v1/reading-evaluations` and stores the result immediately.
  - Failed evaluations can be retried.
- Guardian progress dashboard:
  - Summary.
  - Day timeseries.
  - Difficult words.
  - Session list.
  - Session detail with recordings and evaluation payloads.

## Config

Scoring config under `lexease.scoring`:

- `SCORING_BASE_URL`: external AI server base URL. Default: `http://localhost:6543`.
- `SCORING_MODE`: `ASYNC` by default; `SYNC` uses immediate provider result.
- `SCORING_PROVIDER`: provider label sent to AI server. Default: `gemini`.
- `SCORING_MODEL`: temporary fixed model name. Default: `gemini-configured-model`.
- `SCORING_PROMPT_VERSION`: stored on evaluations. Default: `reading-evaluation-v1`.
- `SCORING_CALLBACK_BASE_URL`: backend base URL used in async callback URL.
- Scoring callbacks use the shared TTS callback HMAC config: `TTS_CALLBACK_SECRET` and `TTS_CALLBACK_TIMESTAMP_TOLERANCE`.
- `SCORING_RECORDING_RETENTION`: recording expiry duration. Default: `30d`.

## Endpoint: `POST /sessions/{sessionId}/recordings`

Creates a recording for the authenticated child and immediately creates/submits an evaluation.

Input:

```json
{
  "durationMs": 1200,
  "expectedText": "Ngày xưa",
  "voice": {
    "mimeType": "audio/webm",
    "contentBase64": "..."
  }
}
```

Output includes recording metadata, signed audio URL, and latest evaluation.

Implementation:

- Only the child who owns the reading session can create a recording.
- Frontend sends `expectedText` for the exact segment being read; backend validates that normalized text belongs to the persisted story before scoring.
- The AI server receives only the validated expected segment, not the full story unless the child recorded the full story.
- Audio is stored under `recordings/{childId}/{recordingId}/audio.bin`.

## Endpoint: Recording/Evaluation CRUD

- `GET /recordings/{id}`: child owner, accepted guardian, or admin.
- `PATCH /recordings/{id}`: updates recording duration metadata.
- `DELETE /recordings/{id}`: soft-deletes recording and latest evaluation.
- `GET /recordings/{id}/evaluation`: reads latest evaluation for a recording.
- `GET /evaluations/{id}`: reads an evaluation by id.
- `POST /evaluations/{id}/retry`: accepted guardian/admin can retry a failed evaluation.
- `DELETE /evaluations/{id}`: accepted guardian/admin can soft-delete evaluation.

## Endpoint: AI Server Callback

`POST /internal/scoring/evaluations/{evaluationId}/callback`

Input headers:

- `X-Lexease-Timestamp`: epoch seconds.
- `X-Lexease-Signature`: `sha256=<hex-hmac>` for `<timestamp>.<raw-json-body>`.

Input body:

```json
{
  "requestId": "evaluation-uuid",
  "status": "DONE",
  "heardText": "Ngày xưa",
  "summary": "Đọc rõ.",
  "scores": {
    "accuracy": 0.9,
    "fluency": 0.8,
    "pace": 0.7
  },
  "words": [
    {
      "wordIndex": 0,
      "expected": "Ngày",
      "heard": "Ngày",
      "correct": true
    }
  ],
  "difficultWords": ["xưa"],
  "error": null
}
```

Output: HTTP `204 No Content`.

Implementation:

- Endpoint is unauthenticated at JWT level but protected by the same HMAC signature and timestamp tolerance used by TTS callbacks.
- Path evaluation id must match body `requestId` when present.
- `DONE` stores heard text, summary, scores, word results, and difficult words.
- `FAILED` stores the provider error message.

## Endpoint: Guardian Progress

- `GET /children/{childId}/progress/summary?range=week|month`
- `GET /children/{childId}/progress/timeseries?range=week|month`
- `GET /children/{childId}/progress/difficult-words?range=week|month`
- `GET /children/{childId}/progress/sessions?range=week|month`
- `GET /children/{childId}/progress/sessions/{sessionId}`

Metrics:

- practice minutes from `reading_sessions.elapsed_ms`.
- sessions and completed sessions from `reading_sessions`.
- recorded sessions from `recordings`.
- accuracy/fluency/pace/errors/difficult words from `ai_evaluations`.
- TTS help count from `reading_events`.
- trends compare current range to the previous range of the same length.

## Verification

- `./gradlew compileJava`
- `./gradlew test`

Added focused tests in `RecordingServiceTests` for:

- child recording creation and async evaluation queueing.
- scoring callback persistence.
- guardian progress summary.
- guardian access denial without accepted child link.
