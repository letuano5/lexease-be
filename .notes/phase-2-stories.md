# Phase 2 - Story Management

Mục tiêu: admin thêm/sửa/xoá truyện; phụ huynh/trẻ search/filter; trẻ không thấy truyện bị guardian chặn. `genre` và `author` là mảng.

## Input

Create story:

```json
{
  "title": "Chu meo di hoc",
  "content": "Ngay xua co mot chu meo...",
  "genreIds": ["uuid"],
  "authorIds": ["uuid"],
  "status": "PUBLISHED"
}
```

Update story:

```json
{
  "title": "Ten moi",
  "content": "Noi dung moi",
  "genreIds": ["uuid"],
  "authorIds": [],
  "status": "DRAFT"
}
```

Search/filter:

```http
GET /stories?keyword=meo&genreId=uuid&authorId=uuid&page=0&size=20
```

Block story:

```json
{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": true,
  "reason": "Khong phu hop do tuoi"
}
```

## Output

Story detail:

```json
{
  "id": "uuid",
  "title": "Chu meo di hoc",
  "content": "Ngay xua co mot chu meo...",
  "genres": [{ "id": "uuid", "name": "Thieu nhi" }],
  "authors": [{ "id": "uuid", "name": "Nguyen A" }],
  "status": "PUBLISHED",
  "version": 1,
  "ttsStatus": "PENDING"
}
```

Search result:

```json
{
  "items": [
    {
      "id": "uuid",
      "title": "Chu meo di hoc",
      "genres": [{ "id": "uuid", "name": "Thieu nhi" }],
      "authors": [{ "id": "uuid", "name": "Nguyen A" }],
      "status": "PUBLISHED",
      "isBlockedForCurrentChild": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 120,
  "totalPages": 6
}
```

## Data Model

`stories`

- `id uuid primary key`
- `title text not null`
- `normalized_title text not null`
- `content text not null`
- `status text not null check in ('DRAFT','PUBLISHED','ARCHIVED')`
- `version integer not null default 1`
- `created_by uuid not null references users(id)`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`genres`

- `id uuid primary key`
- `name text not null`
- `normalized_name text not null unique`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `deleted_at timestamptz null`

`authors`

- `id uuid primary key`
- `name text not null`
- `normalized_name text not null unique`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `deleted_at timestamptz null`

`story_genres`

- `story_id uuid not null references stories(id)`
- `genre_id uuid not null references genres(id)`
- primary key `(story_id, genre_id)`

`story_authors`

- `story_id uuid not null references stories(id)`
- `author_id uuid not null references authors(id)`
- primary key `(story_id, author_id)`

`story_words`

- `id uuid primary key`
- `story_id uuid not null references stories(id)`
- `word_index integer not null`
- `text text not null`
- `normalized_text text not null`
- `start_char integer not null`
- `end_char integer not null`

`story_access_blocks`

- `id uuid primary key`
- `child_id uuid not null references users(id)`
- `story_id uuid not null references stories(id)`
- `blocked_by_guardian_id uuid not null references users(id)`
- `reason text null`
- `active boolean not null default true`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

## Implementation

### 2.1 CRUD story

- Chỉ `ADMIN` được create/update/delete.
- Delete là soft delete:
  - Nếu story chưa có reading session: có thể `ARCHIVED`.
  - Không hard delete trong MVP để tránh mất dữ liệu progress/TTS.
- Khi update content:
  - increment `version`.
  - rebuild `story_words`.
  - invalidate TTS asset của version cũ.
  - enqueue TTS generation ở Phase 5.

### 2.2 Text normalization/tokenization

Tạo `StoryTextProcessor`:

- normalize title để search.
- split word.
- giữ `start_char/end_char` để frontend map highlight.
- tiếng Việt cần giữ dấu trong `text`, chỉ normalized field dùng cho search/filter.

### 2.3 Genres/authors as arrays

Không lưu `genre text` và `author text` trong `stories`.

MVP dùng bảng riêng `genres`, `authors` và join table `story_genres`, `story_authors`.
Frontend gọi API metadata lấy UUID trước, sau đó create/update/filter story bằng UUID.
Admin quản lý genre/author qua endpoint riêng. Delete là soft delete bằng `deleted_at`.

### 2.4 Search/filter

MVP:

- filter status theo role.
- keyword search trên `normalized_title`.
- filter genre/author bằng `genreId` và `authorId` qua join table.
- page/size có limit max.

PostgreSQL index:

- index `stories(status)`.
- index `story_genres(genre_id)`.
- index `story_authors(author_id)`.
- cân nhắc `pg_trgm` cho title sau khi dataset lớn hơn.

### 2.5 Child visibility

Child chỉ thấy story:

- `status = PUBLISHED`.
- không có active block từ guardian đang link `ACCEPTED`.

Rule MVP đề xuất: chỉ cần một accepted guardian block thì child không thấy story.

Decision: block toàn cục cho child nếu blocker là accepted guardian.

### 2.6 Guardian block/unblock

- Guardian chỉ block được child đã link `ACCEPTED`.
- Unblock chỉ nên cho guardian đã tạo block hoặc admin.
- Audit action block/unblock.

## APIs

- `POST /admin/stories`
- `PATCH /admin/stories/{id}`
- `DELETE /admin/stories/{id}`
- `GET /stories`
- `GET /stories/{id}`
- `GET /genres`
- `POST /genres`
- `PATCH /genres/{id}`
- `DELETE /genres/{id}`
- `GET /authors`
- `POST /authors`
- `PATCH /authors/{id}`
- `DELETE /authors/{id}`
- `POST /story-access/block`
- `POST /story-access/unblock`

## Permission Matrix

- `POST /admin/stories`: admin only. Mutates `stories`, `story_words`, `story_genres`, and `story_authors`. Does not create/update/delete `genres` or `authors`; supplied IDs must already exist.
- `PATCH /admin/stories/{id}`: admin only. Mutates the target story and replaces its genre/author links and words.
- `DELETE /admin/stories/{id}`: admin only. Soft deletes by setting story status to `ARCHIVED`.
- `GET /stories`: authenticated users. Admin can see all story statuses. Guardian/child see only `PUBLISHED`. Child context rules apply when `childId` is supplied or actor is a child.
- `GET /stories/{id}`: authenticated users. Admin can read any status. Guardian/child can read only `PUBLISHED`, and blocked stories are hidden when a child context is active.
- `GET /genres`: authenticated users. Read-only metadata list. No mutation.
- `GET /authors`: authenticated users. Read-only metadata list. No mutation.
- `POST /genres`, `PATCH /genres/{id}`, `DELETE /genres/{id}`: admin only. Mutates only `genres`; delete is soft delete.
- `POST /authors`, `PATCH /authors/{id}`, `DELETE /authors/{id}`: admin only. Mutates only `authors`; delete is soft delete.
- `POST /story-access/block`: guardian only, and only for an `ACCEPTED` child link. Mutates only `story_access_blocks`, not the story itself.
- `POST /story-access/unblock`: accepted guardian or admin. Guardian can deactivate only their own active block; admin can deactivate all active blocks for the child/story pair.

Story create/update accepts only non-deleted genre/author IDs. `GET /genres` and `GET /authors` return only non-deleted metadata. Soft-deleted metadata may remain linked to existing stories for history/display consistency.

## Done Criteria

- Admin CRUD story pass validation.
- `genres` và `authors` trả về dạng array.
- Update content rebuild words và version.
- Child search không trả story bị block.
- Guardian không thể block child chưa accepted link.
- Có test search/filter cho genre và author nhiều giá trị.
