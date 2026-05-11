create table users (
    id uuid primary key,
    email text not null unique,
    password_hash text not null,
    display_name text not null,
    role text not null check (role in ('ADMIN', 'GUARDIAN', 'CHILD')),
    status text not null check (status in ('ACTIVE', 'DISABLED')),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null unique,
    device_id text null,
    expires_at timestamptz not null,
    revoked_at timestamptz null,
    created_at timestamptz not null
);

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);

create table guardian_child_links (
    id uuid primary key,
    guardian_id uuid not null references users(id) on delete cascade,
    child_id uuid not null references users(id) on delete cascade,
    status text not null check (status in ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED')),
    invited_by uuid not null references users(id),
    created_at timestamptz not null,
    accepted_at timestamptz null,
    check (guardian_id <> child_id)
);

create index idx_guardian_child_links_guardian_id on guardian_child_links(guardian_id);
create index idx_guardian_child_links_child_id on guardian_child_links(child_id);
create unique index uq_guardian_child_links_active_pair
    on guardian_child_links(guardian_id, child_id)
    where status <> 'REVOKED';

create table audit_logs (
    id uuid primary key,
    actor_user_id uuid null references users(id),
    action text not null,
    target_type text not null,
    target_id uuid null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null
);

create index idx_audit_logs_actor_user_id on audit_logs(actor_user_id);
create index idx_audit_logs_created_at on audit_logs(created_at);
