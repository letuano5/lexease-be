# Phase 6 - Gemini Scoring And Progress Analytics

Mục tiêu: child upload recording, backend gọi Gemini để đánh giá bài đọc, lưu transcript/score/errors/difficult words, rồi tổng hợp dashboard tiến độ.

## Input

Create upload session:

```json
{
  "sessionId": "uuid",
  "mimeType": "audio/webm",
  "durationEstimateMs": 120000
}
```

Complete upload:

```json
{
  "recordingId": "uuid",
  "durationMs": 118234,
  "fileSize": 1839201
}
```

Internal Gemini job:

```json
{
  "recordingId": "uuid",
  "storyText": "Ngay xua co mot chu meo...",
  "model": "configured-model-name"
}
```

Progress summary:

```http
GET /children/{childId}/progress/summary?range=month
```

## Output

Upload session:

```json
{
  "recordingId": "uuid",
  "uploadUrl": "signed-upload-url",
  "objectKey": "recordings/child/session/recording.webm"
}
```

Evaluation result:

```json
{
  "recordingId": "uuid",
  "evaluationStatus": "DONE",
  "summary": "Doc tuong doi ro, ngat nghi tot.",
  "scores": {
    "fluency": 0.82,
    "accuracy": 0.76,
    "pace": 0.68
  },
  "errors": [
    {
      "expected": "chu",
      "heard": "chu",
      "type": "pronunciation",
      "confidence": 0.71
    }
  ],
  "difficultWords": ["truong", "nghi"]
}
```

Progress summary:

```json
{
  "totalPracticeMinutes": 320,
  "sessionsCount": 24,
  "averageReadingSpeedWpm": 58,
  "averageErrorsPerSession": 7.2,
  "trend": {
    "readingSpeed": "+12%",
    "errors": "-18%"
  }
}
```

## Data Model

`recordings`

- `id uuid primary key`
- `session_id uuid not null references reading_sessions(id)`
- `child_id uuid not null references users(id)`
- `story_id uuid not null references stories(id)`
- `object_key text not null`
- `duration_ms integer null`
- `file_size bigint null`
- `mime_type text not null`
- `upload_status text not null check in ('PENDING','UPLOADED','FAILED')`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`ai_model_configs`

- `id uuid primary key`
- `task text not null`
- `provider text not null`
- `model_name text not null`
- `enabled boolean not null default true`
- `temperature numeric(3,2) null`
- `response_schema jsonb null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`ai_evaluations`

- `id uuid primary key`
- `recording_id uuid not null references recordings(id)`
- `provider text not null`
- `model_name text not null`
- `prompt_version text not null`
- `status text not null check in ('PENDING','PROCESSING','DONE','FAILED')`
- `transcript text null`
- `summary text null`
- `score_json jsonb null`
- `errors_json jsonb null`
- `difficult_words_json jsonb null`
- `raw_response_object_key text null`
- `error_message text null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

Optional aggregate table after raw events are stable:

`child_daily_progress`

- `child_id uuid not null`
- `date date not null`
- `practice_minutes integer not null`
- `sessions_count integer not null`
- `average_wpm numeric null`
- `error_count integer not null`
- `tts_help_count integer not null`
- primary key `(child_id, date)`

## Implementation

### 6.1 Recording upload

- Child creates upload session for own reading session.
- Backend returns signed upload URL.
- Frontend uploads directly to storage.
- Frontend calls complete upload.
- Backend marks recording `UPLOADED` and creates evaluation job.

### 6.2 Gemini config

Do not hard-code model id.

Load active config by task:

```text
task = READING_EVALUATION
provider = GEMINI
model_name = from DB/env
```

Seed default via Flyway or app config, but allow changing without recompiling.

### 6.3 Prompt and response schema

Prompt should include:

- Story text expected.
- Child audio file.
- Required JSON schema.
- Scoring rubric for fluency/accuracy/pace.
- Instruction to return uncertain results with low confidence.

Store `prompt_version` so future prompt changes do not mix metrics silently.

### 6.4 Evaluation job

Flow:

1. Load recording + reading session + story text.
2. Upload/pass audio to Gemini according to file size.
3. Ask Gemini for structured JSON.
4. Validate JSON schema.
5. Save transcript, summary, scores, errors, difficult words.
6. Mark failed with retryable error if provider/network fails.

Important: Gemini scoring should be treated as assistive feedback, not ground truth, until benchmarked with real recordings.

### 6.5 Progress analytics

Metrics from:

- `reading_sessions.elapsed_ms`.
- `reading_events` for TTS help count.
- `ai_evaluations.errors_json`.
- `ai_evaluations.difficult_words_json`.

Endpoints:

- summary by range.
- timeseries by day/week/month.
- difficult words.
- session detail with recording playback URL and evaluation.

### 6.6 Retention and privacy

Recording is sensitive child data.

Decision needed:

- retention: 30/90/180 days or manual delete only.
- whether guardian must consent before sending recording to Gemini.

MVP should at least store `created_at` and make deletion possible later.

## APIs

- `POST /recordings/upload-session`
- `POST /recordings/{id}/complete-upload`
- `GET /recordings/{id}`
- `GET /reading-sessions/{sessionId}`
- `GET /children/{childId}/progress/summary`
- `GET /children/{childId}/progress/timeseries`
- `GET /children/{childId}/progress/difficult-words`
- Admin/internal retry: `POST /admin/evaluations/{id}/retry`

## Done Criteria

- Child uploads recording for own session.
- Guardian accepted child can view recording/evaluation.
- Gemini model id is configurable.
- Evaluation job persists structured result.
- Failed evaluation can be retried.
- Progress summary/timeseries/difficult words work from stored sessions/events/evaluations.
- Recording URL is signed and short-lived.

