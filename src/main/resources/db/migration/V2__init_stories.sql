create table genres (
    id uuid primary key,
    name text not null,
    normalized_name text not null unique,
    created_at timestamptz not null
);

create table authors (
    id uuid primary key,
    name text not null,
    normalized_name text not null unique,
    created_at timestamptz not null
);

create table stories (
    id uuid primary key,
    title text not null,
    normalized_title text not null,
    content text not null,
    status text not null check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    version integer not null default 1,
    created_by uuid not null references users(id),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_stories_status on stories(status);
create index idx_stories_normalized_title on stories(normalized_title);

create table story_genres (
    story_id uuid not null references stories(id) on delete cascade,
    genre_id uuid not null references genres(id),
    primary key (story_id, genre_id)
);

create index idx_story_genres_genre_id on story_genres(genre_id);

create table story_authors (
    story_id uuid not null references stories(id) on delete cascade,
    author_id uuid not null references authors(id),
    primary key (story_id, author_id)
);

create index idx_story_authors_author_id on story_authors(author_id);

create table story_words (
    id uuid primary key,
    story_id uuid not null references stories(id) on delete cascade,
    word_index integer not null,
    text text not null,
    normalized_text text not null,
    start_char integer not null,
    end_char integer not null
);

create unique index uq_story_words_story_word_index on story_words(story_id, word_index);

create table story_access_blocks (
    id uuid primary key,
    child_id uuid not null references users(id) on delete cascade,
    story_id uuid not null references stories(id) on delete cascade,
    blocked_by_guardian_id uuid not null references users(id) on delete cascade,
    reason text null,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_story_access_blocks_child_story_active
    on story_access_blocks(child_id, story_id, active);
create index idx_story_access_blocks_guardian_id
    on story_access_blocks(blocked_by_guardian_id);
create unique index uq_story_access_blocks_active_guardian_child_story
    on story_access_blocks(blocked_by_guardian_id, child_id, story_id)
    where active;
