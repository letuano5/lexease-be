create table tts_assets (
    id uuid primary key,
    story_id uuid not null references stories(id) on delete cascade,
    story_version integer not null,
    provider text not null,
    provider_request_id text null,
    provider_job_id text null,
    voice_id text not null,
    audio_object_key text null,
    audio_mime_type text null,
    audio_duration_ms integer null,
    audio_sample_rate_hz integer null,
    status text not null check (status in ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'INVALIDATED')),
    error_message text null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index uq_tts_assets_story_version_voice
    on tts_assets(story_id, story_version, voice_id);
create index idx_tts_assets_story_version
    on tts_assets(story_id, story_version);
create index idx_tts_assets_status
    on tts_assets(status);
create index idx_tts_assets_provider_request_id
    on tts_assets(provider_request_id);
create index idx_tts_assets_provider_job_id
    on tts_assets(provider_job_id);

create table tts_word_timings (
    id uuid primary key,
    tts_asset_id uuid not null references tts_assets(id) on delete cascade,
    story_word_id uuid null references story_words(id),
    word_index integer not null,
    text text not null,
    start_ms integer not null,
    end_ms integer not null,
    start_char integer null,
    end_char integer null
);

create unique index uq_tts_word_timings_asset_word_index
    on tts_word_timings(tts_asset_id, word_index);
create index idx_tts_word_timings_asset_id
    on tts_word_timings(tts_asset_id);

create table reading_sessions (
    id uuid primary key,
    child_id uuid not null references users(id) on delete cascade,
    story_id uuid not null references stories(id) on delete cascade,
    story_version integer not null,
    voice_id text not null,
    status text not null check (status in ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    current_word_index integer not null default 0,
    elapsed_ms bigint not null default 0,
    started_at timestamptz not null,
    last_active_at timestamptz not null,
    completed_at timestamptz null
);

create index idx_reading_sessions_child_story_voice_status
    on reading_sessions(child_id, story_id, voice_id, status);
create index idx_reading_sessions_child_last_active
    on reading_sessions(child_id, last_active_at desc);

create table reading_events (
    id uuid primary key,
    session_id uuid not null references reading_sessions(id) on delete cascade,
    event_type text not null,
    word text null,
    word_index integer null,
    timestamp_ms bigint null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null
);

create index idx_reading_events_session_created_at
    on reading_events(session_id, created_at);
