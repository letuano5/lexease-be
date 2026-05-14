create table display_settings (
    id uuid primary key,
    child_id uuid not null unique references users(id) on delete cascade,
    font_family text not null,
    font_size integer not null,
    line_height numeric(4,2) not null,
    letter_spacing numeric(4,2) not null,
    background_color text not null,
    text_color text not null,
    theme_name text null,
    settings_version integer not null default 1,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_display_settings_child_id on display_settings(child_id);
