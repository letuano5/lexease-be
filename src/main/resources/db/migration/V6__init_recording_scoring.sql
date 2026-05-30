create table recordings (
    id uuid primary key,
    session_id uuid not null references reading_sessions(id) on delete cascade,
    child_id uuid not null references users(id) on delete cascade,
    story_id uuid not null references stories(id) on delete cascade,
    status text not null check (status in ('READY', 'DELETED')),
    duration_ms bigint null,
    word_count integer not null default 0,
    expires_at timestamptz not null,
    deleted_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_recordings_child_created_at
    on recordings(child_id, created_at desc)
    where deleted_at is null;
create index idx_recordings_session_id
    on recordings(session_id)
    where deleted_at is null;
create index idx_recordings_expires_at
    on recordings(expires_at)
    where deleted_at is null;
create index idx_reading_sessions_child_started_at
    on reading_sessions(child_id, started_at desc);
create index idx_reading_events_type_created_at
    on reading_events(event_type, created_at);

create table recording_words (
    id uuid primary key,
    recording_id uuid not null references recordings(id) on delete cascade,
    word_index integer not null,
    expected_text text not null,
    audio_object_key text not null,
    audio_mime_type text not null,
    duration_ms integer null,
    client_started_at_ms bigint null,
    client_ended_at_ms bigint null,
    created_at timestamptz not null
);

create unique index uq_recording_words_recording_word_index
    on recording_words(recording_id, word_index);
create index idx_recording_words_recording_id
    on recording_words(recording_id);

create table ai_evaluations (
    id uuid primary key,
    recording_id uuid not null references recordings(id) on delete cascade,
    provider text not null,
    model_name text not null,
    prompt_version text not null,
    provider_job_id text null,
    status text not null check (status in ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    heard_text text null,
    summary text null,
    scores jsonb not null default '{}'::jsonb,
    word_results jsonb not null default '[]'::jsonb,
    difficult_words jsonb not null default '[]'::jsonb,
    error_message text null,
    deleted_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_ai_evaluations_recording_created_at
    on ai_evaluations(recording_id, created_at desc)
    where deleted_at is null;
create index idx_ai_evaluations_status
    on ai_evaluations(status)
    where deleted_at is null;
