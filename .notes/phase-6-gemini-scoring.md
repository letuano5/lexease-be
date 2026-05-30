# Phase 6 - Gemini Scoring And Progress Analytics

Mục tiêu: child đọc xong một đoạn/bài, bấm stop, gửi đoạn text đang đọc và một file thu âm duy nhất; Spring validate đoạn text thuộc story, lưu recording, gửi expected text + audio sang AI server, AI server gọi Gemini để chấm điểm, rồi Spring lưu kết quả và cung cấp dashboard tiến độ cho guardian.

Decision đã chốt:

- Frontend gửi một `voice` audio payload cho toàn bộ recording.
- Frontend gửi `expectedText` là đoạn text child vừa đọc.
- Spring validate `expectedText` thuộc story của reading session trước khi gửi đi chấm.
- Spring backend lưu một audio object cho mỗi recording.
- Scoring ưu tiên async qua AI server; có sync fallback bằng config.
- Không code Gemini trực tiếp trong Spring.
- Không làm `ai_model_configs` CRUD trong phase này; provider/model fix qua config/env tạm thời.
- Không cần consent riêng trước khi gửi audio sang AI server/Gemini.
- Retention mặc định 30 ngày hoặc guardian chủ động xoá.
- Dashboard chính dành cho guardian theo dõi child.

## Input

Create recording for a reading session:

```http
POST /sessions/{sessionId}/recordings
```

```json
{
  "durationMs": 58200,
  "expectedText": "Ngay xua co mot chu meo...",
  "voice": {
    "mimeType": "audio/webm",
    "contentBase64": "..."
  }
}
```

Spring calls AI server async:

```json
{
  "requestId": "evaluation-uuid",
  "callbackUrl": "https://api.example.com/internal/scoring/evaluations/{evaluationId}/callback",
  "childId": "uuid",
  "sessionId": "uuid",
  "recordingId": "uuid",
  "story": {
    "id": "uuid",
    "title": "Chu meo di hoc",
    "expectedText": "Ngay xua co mot chu meo..."
  },
  "audio": {
    "audioUrl": "signed-read-url",
    "mimeType": "audio/webm",
    "durationMs": 58200
  },
  "language": "vi-VN",
  "provider": "gemini",
  "model": "configured-model-name",
  "promptVersion": "reading-evaluation-v1"
}
```

AI server callback:

```http
POST /internal/scoring/evaluations/{evaluationId}/callback
X-Lexease-Timestamp: 1770000000
X-Lexease-Signature: sha256=<hmac>
```

```json
{
  "requestId": "evaluation-uuid",
  "status": "DONE",
  "heardText": "abc ...",
  "summary": "Doc ro, con sai mot so am cuoi.",
  "scores": {
    "accuracy": 0.82,
    "fluency": 0.74,
    "pace": 0.69
  },
  "words": [
    {
      "wordIndex": 0,
      "expected": "abc",
      "heard": "abc",
      "correct": true,
      "confidence": 0.91,
      "errorType": null
    }
  ],
  "difficultWords": ["..."],
  "error": null
}
```

Progress summary:

```http
GET /children/{childId}/progress/summary?range=month
```

## Data Model

`recordings`

- `id uuid primary key`
- `session_id uuid not null references reading_sessions(id)`
- `child_id uuid not null references users(id)`
- `story_id uuid not null references stories(id)`
- `status text not null check in ('READY','DELETED')`
- `duration_ms bigint null`
- `word_count integer not null`
- `expected_text text not null`
- `audio_object_key text not null`
- `audio_mime_type text not null`
- `expires_at timestamptz not null`
- `deleted_at timestamptz null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`ai_evaluations`

- `id uuid primary key`
- `recording_id uuid not null references recordings(id)`
- `provider text not null`
- `model_name text not null`
- `prompt_version text not null`
- `provider_job_id text null`
- `status text not null check in ('PENDING','PROCESSING','DONE','FAILED')`
- `heard_text text null`
- `summary text null`
- `scores jsonb not null default '{}'`
- `word_results jsonb not null default '[]'`
- `difficult_words jsonb not null default '[]'`
- `error_message text null`
- `deleted_at timestamptz null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

## APIs

Recording and evaluation:

- `POST /sessions/{sessionId}/recordings`
- `GET /recordings/{id}`
- `PATCH /recordings/{id}`
- `DELETE /recordings/{id}`
- `GET /recordings/{id}/evaluation`
- `GET /evaluations/{id}`
- `POST /evaluations/{id}/retry`
- `DELETE /evaluations/{id}`
- `POST /internal/scoring/evaluations/{evaluationId}/callback`

Progress dashboard:

- `GET /children/{childId}/progress/summary`
- `GET /children/{childId}/progress/timeseries`
- `GET /children/{childId}/progress/difficult-words`
- `GET /children/{childId}/progress/sessions`
- `GET /children/{childId}/progress/sessions/{sessionId}`

## Progress Metrics

Dashboard summary should expose:

- `totalPracticeMinutes`
- `sessionsCount`
- `completedSessionsCount`
- `recordedSessionsCount`
- `averageReadingSpeedWpm`
- `averageAccuracy`
- `averageFluency`
- `averagePace`
- `averageErrorsPerSession`
- `ttsHelpCount`
- trends versus previous range for practice time, reading speed, accuracy, and errors.

Session detail should expose story, elapsed time, current/completed position, recordings, playback signed URLs, evaluation summary, expected/heard word results, and difficult words.

## Done Criteria

- Child creates recording for own reading session with a single audio payload.
- Recording audio is stored and returned through signed URLs.
- Spring submits evaluation to AI server async and supports sync fallback by config.
- AI callback persists heard text, summary, scores, word-level results, and difficult words.
- Failed evaluation can be retried.
- Child owner and accepted guardian can view recording/evaluation.
- Guardian can soft-delete recordings.
- Progress summary/timeseries/difficult words/session detail work from stored sessions/events/evaluations.
- Recording retention is configurable and defaults to 30 days.
