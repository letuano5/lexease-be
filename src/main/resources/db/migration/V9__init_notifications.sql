create table device_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    platform text not null check (platform in ('IOS', 'ANDROID')),
    device_id text null,
    token text not null,
    active boolean not null default true,
    last_seen_at timestamptz not null,
    created_at timestamptz not null
);

create index idx_device_tokens_user_active
    on device_tokens(user_id, active);
create unique index uq_device_tokens_token
    on device_tokens(token);
create unique index uq_device_tokens_active_device
    on device_tokens(user_id, platform, device_id)
    where active = true and device_id is not null;

create table reminder_schedules (
    id uuid primary key,
    guardian_id uuid not null references users(id) on delete cascade,
    child_id uuid not null references users(id) on delete cascade,
    days_of_week text[] not null,
    local_time time not null,
    timezone text not null,
    message text not null,
    enabled boolean not null default true,
    next_run_at timestamptz null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (cardinality(days_of_week) > 0)
);

create index idx_reminder_schedules_enabled_next_run
    on reminder_schedules(enabled, next_run_at);
create index idx_reminder_schedules_child_id
    on reminder_schedules(child_id);
create index idx_reminder_schedules_guardian_id
    on reminder_schedules(guardian_id);

create table notification_events (
    id uuid primary key,
    schedule_id uuid null references reminder_schedules(id) on delete set null,
    child_id uuid not null references users(id) on delete cascade,
    status text not null check (status in (
        'SCHEDULED',
        'SENT',
        'FAILED',
        'OPENED_ON_TIME',
        'OPENED_LATE',
        'PRACTICE_STARTED',
        'IGNORED'
    )),
    deep_link text not null,
    scheduled_for timestamptz not null,
    sent_at timestamptz null,
    opened_at timestamptz null,
    practice_started_at timestamptz null,
    failure_reason text null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_notification_events_status_scheduled_for
    on notification_events(status, scheduled_for);
create index idx_notification_events_child_id
    on notification_events(child_id);
create unique index uq_notification_events_schedule_scheduled_for
    on notification_events(schedule_id, scheduled_for)
    where schedule_id is not null;
