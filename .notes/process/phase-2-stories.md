# Phase 2 - Story Management Process

## Implemented Features

- Flyway schema for story management in `V2__init_stories.sql`:
  - `genres`
  - `authors`
  - `stories`
  - `story_genres`
  - `story_authors`
  - `story_words`
  - `story_access_blocks`
- Admin story create/update/archive.
- Story title normalization for accent-insensitive keyword search.
- Word splitting with `startChar`/`endChar` offsets for word-level highlighting.
- Genres and authors stored as independent metadata entities.
- Story-to-genre and story-to-author links stored through join tables.
- Authenticated story search with keyword, `genreId`, and `authorId` filters.
- Metadata endpoints for frontend filters/forms:
  - `GET /genres`
  - `GET /authors`
- Admin metadata management endpoints:
  - `POST /genres`
  - `PATCH /genres/{id}`
  - `DELETE /genres/{id}`
  - `POST /authors`
  - `PATCH /authors/{id}`
  - `DELETE /authors/{id}`
- Code is split by domain:
  - `com.lexease.stories`: story entity, story endpoints, story words, story access blocks.
  - `com.lexease.genres`: genre entity, repository, service, controller, DTOs.
  - `com.lexease.authors`: author entity, repository, service, controller, DTOs.
- Child visibility rule excludes stories with an active block from an accepted guardian.
- Guardian block/unblock for accepted children.
- Admin can unblock all active blocks for a child/story.
- Audit actions for create/update/archive/block/unblock.

## Data Model

Implemented in `V2__init_stories.sql`.

`genres`

- `id`: UUID primary key.
- `name`: display name.
- `normalized_name`: unique normalized name for de-dup/search support.
- `created_at`: creation timestamp.
- `updated_at`: last update timestamp.
- `deleted_at`: soft delete timestamp. `null` means active.

`authors`

- `id`: UUID primary key.
- `name`: display name.
- `normalized_name`: unique normalized name for de-dup/search support.
- `created_at`: creation timestamp.
- `updated_at`: last update timestamp.
- `deleted_at`: soft delete timestamp. `null` means active.

`stories`

- `id`: UUID primary key.
- `title`: display title.
- `normalized_title`: normalized title for keyword search.
- `content`: full story text.
- `status`: `DRAFT`, `PUBLISHED`, or `ARCHIVED`.
- `version`: increments on update/archive.
- `created_by`: admin user who created the story.
- `created_at`, `updated_at`: audit timestamps.

`story_genres`

- Join table between `stories` and `genres`.
- Primary key: `(story_id, genre_id)`.

`story_authors`

- Join table between `stories` and `authors`.
- Primary key: `(story_id, author_id)`.

`story_words`

- One row per tokenized word.
- Stores `word_index`, original `text`, `normalized_text`, `start_char`, and `end_char`.
- No block/paragraph/sentence model is stored; highlighting is word-level only.

`story_access_blocks`

- Stores guardian story blocks per child.
- Active blocks hide published stories from the child when the blocker remains an accepted guardian.

## Permission Matrix

All endpoints except `/actuator/health`, `/actuator/info`, `/auth/register`, `/auth/login`, and `/auth/refresh` require authentication through `SecurityConfig`.

| Endpoint | Allowed actors | Mutates | Rule |
| --- | --- | --- | --- |
| `POST /admin/stories` | Admin only | `stories`, `story_words`, `story_genres`, `story_authors` | `@PreAuthorize("hasRole('ADMIN')")`; `genreIds` and `authorIds` must already exist. |
| `PATCH /admin/stories/{id}` | Admin only | target story, word rows, genre/author links | Replaces content, status, metadata links, and words; increments version. |
| `DELETE /admin/stories/{id}` | Admin only | target story status | Soft delete by setting `ARCHIVED`; increments version. |
| `GET /stories` | Authenticated admin/guardian/child | none | Admin sees all statuses; guardian/child see only `PUBLISHED`; child context excludes active accepted-guardian blocks. |
| `GET /stories/{id}` | Authenticated admin/guardian/child | none | Admin can read all statuses; guardian/child can read only `PUBLISHED`; child context hides blocked story. |
| `GET /genres` | Authenticated admin/guardian/child | none | Read-only metadata list. |
| `GET /authors` | Authenticated admin/guardian/child | none | Read-only metadata list. |
| `POST /genres` | Admin only | `genres` | Creates active genre; duplicate normalized names return `GENRE_ALREADY_EXISTS`. |
| `PATCH /genres/{id}` | Admin only | `genres` | Updates active genre name and normalized name. |
| `DELETE /genres/{id}` | Admin only | `genres` | Soft deletes by setting `deleted_at`; does not remove story links. |
| `POST /authors` | Admin only | `authors` | Creates active author; duplicate normalized names return `AUTHOR_ALREADY_EXISTS`. |
| `PATCH /authors/{id}` | Admin only | `authors` | Updates active author name and normalized name. |
| `DELETE /authors/{id}` | Admin only | `authors` | Soft deletes by setting `deleted_at`; does not remove story links. |
| `POST /story-access/block` | Guardian only | `story_access_blocks` | Guardian must have `ACCEPTED` link to child; story row itself is not mutated. |
| `POST /story-access/unblock` | Admin, or accepted guardian | `story_access_blocks` | Guardian can deactivate only own active block; admin can deactivate all active blocks for child/story. |

Important metadata constraint:

- Admin story create/update can only link to existing, non-deleted metadata IDs.
- `GET /genres` and `GET /authors` return only non-deleted metadata.
- Soft-deleted metadata can remain linked to existing stories for history/display consistency.

## Endpoint: `POST /admin/stories`

Creates a story. Admin only.

Input:

```json
{
  "title": "Chu meo di hoc",
  "content": "Ngay xua co mot chu meo...",
  "genreIds": ["uuid"],
  "authorIds": ["uuid"],
  "status": "PUBLISHED"
}
```

Input fields:

- `title`: required, max 300 chars.
- `content`: required story content.
- `genreIds`: required array of existing genre UUIDs.
- `authorIds`: required array of existing author UUIDs.
- `status`: required. `DRAFT`, `PUBLISHED`, or `ARCHIVED`.

Output:

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

Implementation:

- `AdminStoryController` delegates to `StoryService.create`.
- Current admin is stored as `created_by`.
- Title is normalized into `normalized_title`.
- `StoryTextProcessor` tokenizes content into `story_words`.
- Genre/author UUIDs are loaded from active metadata rows and persisted through join tables.
- Missing metadata IDs return `GENRE_NOT_FOUND` or `AUTHOR_NOT_FOUND`.
- `ttsStatus` is returned as `PENDING` until Phase 5 implements TTS assets.
- Audit action: `STORY_CREATED`.

## Endpoint: `PATCH /admin/stories/{id}`

Updates a story. Admin only.

Input: same shape as create.

Output: same shape as create.

Implementation:

- Loads story by UUID.
- Replaces title, normalized title, content, status, genre links, author links, and word rows.
- Increments `version` on every admin update.
- Audit action: `STORY_UPDATED`.

## Endpoint: `DELETE /admin/stories/{id}`

Archives a story. Admin only.

Input:

- Path `id`: story UUID.

Output: HTTP `204 No Content`.

Implementation:

- Soft delete only: status becomes `ARCHIVED`.
- Increments `version`.
- Audit action: `STORY_ARCHIVED`.

## Endpoint: `GET /stories`

Searches stories.

Input:

```http
GET /stories?keyword=meo&genreId=uuid&authorId=uuid&childId=uuid&page=0&size=20
```

Input fields:

- `keyword`: optional. Normalized and matched against `normalized_title`.
- `genreId`: optional repeated query param. Matches story genres by UUID.
- `authorId`: optional repeated query param. Matches story authors by UUID.
- `childId`: optional child context for guardian/admin. Child users always use their own id.
- `page`: optional zero-based page index. Default `0`.
- `size`: optional page size, clamped to `1..100`. Default `20`.

Output:

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
  "totalItems": 1,
  "totalPages": 1
}
```

Implementation:

- Admin can see all statuses.
- Guardian and child users only see `PUBLISHED` stories.
- Child users cannot request another child context.
- Guardian can use `childId` only for an accessible child through `ACCEPTED` link.
- If a child context exists, active accepted-guardian blocks are excluded.
- Genre/author filters join through `story_genres` and `story_authors` by metadata UUID.

## Endpoint: `GET /stories/{id}`

Returns story detail.

Input:

- Path `id`: story UUID.
- Optional query `childId`: child context for guardian/admin.

Output: same story detail shape as create.

Implementation:

- Admin can read any story status.
- Guardian and child users cannot read non-published stories.
- Child users cannot request another child context.
- Guardian can use `childId` only for an accessible child through `ACCEPTED` link.
- If a child context exists and the story is blocked, returns `STORY_NOT_FOUND`.

## Endpoint: `GET /genres`

Lists story genres for frontend filters and story create/update forms.

Permission: authenticated users only. Read-only. Soft-deleted genres are excluded.

Output:

```json
[
  {
    "id": "uuid",
    "name": "Thieu nhi"
  }
]
```

## Endpoint: `POST /genres`

Creates a genre. Admin only.

Input:

```json
{
  "name": "Thieu nhi"
}
```

Output:

```json
{
  "id": "uuid",
  "name": "Thieu nhi"
}
```

Implementation:

- Normalizes `name` into `normalized_name`.
- Duplicate normalized names return `GENRE_ALREADY_EXISTS`.
- Audit action: `GENRE_CREATED`.

## Endpoint: `PATCH /genres/{id}`

Updates an active genre. Admin only.

Input/output: same shape as create.

Implementation:

- Deleted genres return `GENRE_NOT_FOUND`.
- Duplicate normalized names return `GENRE_ALREADY_EXISTS`.
- Audit action: `GENRE_UPDATED`.

## Endpoint: `DELETE /genres/{id}`

Soft deletes an active genre. Admin only.

Output: HTTP `204 No Content`.

Implementation:

- Sets `deleted_at` and `updated_at`.
- Does not remove existing `story_genres` links.
- Deleted genres are hidden from `GET /genres` and cannot be used in future story create/update requests.
- Audit action: `GENRE_DELETED`.

## Endpoint: `GET /authors`

Lists story authors for frontend filters and story create/update forms.

Permission: authenticated users only. Read-only. Soft-deleted authors are excluded.

Output:

```json
[
  {
    "id": "uuid",
    "name": "Nguyen A"
  }
]
```

## Endpoint: `POST /authors`

Creates an author. Admin only.

Input:

```json
{
  "name": "Nguyen A"
}
```

Output:

```json
{
  "id": "uuid",
  "name": "Nguyen A"
}
```

Implementation:

- Normalizes `name` into `normalized_name`.
- Duplicate normalized names return `AUTHOR_ALREADY_EXISTS`.
- Audit action: `AUTHOR_CREATED`.

## Endpoint: `PATCH /authors/{id}`

Updates an active author. Admin only.

Input/output: same shape as create.

Implementation:

- Deleted authors return `AUTHOR_NOT_FOUND`.
- Duplicate normalized names return `AUTHOR_ALREADY_EXISTS`.
- Audit action: `AUTHOR_UPDATED`.

## Endpoint: `DELETE /authors/{id}`

Soft deletes an active author. Admin only.

Output: HTTP `204 No Content`.

Implementation:

- Sets `deleted_at` and `updated_at`.
- Does not remove existing `story_authors` links.
- Deleted authors are hidden from `GET /authors` and cannot be used in future story create/update requests.
- Audit action: `AUTHOR_DELETED`.

## Endpoint: `POST /story-access/block`

Blocks a story for a child. Guardian only.

Input:

```json
{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": true,
  "reason": "Khong phu hop do tuoi"
}
```

Input fields:

- `childId`: required child UUID.
- `storyId`: required story UUID.
- `blocked`: optional, but if present must be `true`.
- `reason`: optional, max 500 chars.

Output:

```json
{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": true
}
```

Implementation:

- Actor must be a guardian.
- Guardian must have an `ACCEPTED` link to the child.
- Target child must be an active child account.
- Existing active block by the same guardian is updated idempotently.
- Mutates only `story_access_blocks`; does not mutate `stories`, `genres`, or `authors`.
- Audit action: `STORY_BLOCKED`.

## Endpoint: `POST /story-access/unblock`

Unblocks a story for a child.

Input:

```json
{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": false
}
```

Input fields:

- `childId`: required child UUID.
- `storyId`: required story UUID.
- `blocked`: optional, but if present must be `false`.

Output:

```json
{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": false
}
```

Implementation:

- Guardian must have an `ACCEPTED` link to the child and can unblock only their own active block.
- Admin can deactivate all active blocks for the child/story pair.
- Child cannot unblock story access blocks.
- Mutates only `story_access_blocks`; does not mutate `stories`, `genres`, or `authors`.
- Audit action: `STORY_UNBLOCKED`.

## Verification

- `./gradlew compileJava` passes.
- `./gradlew test` passes.
- PostgreSQL/Flyway was verified before migration squash through local boot run; after squash, reset local/dev DB before reapplying migrations.
- Added tests:
  - Vietnamese search normalization.
  - Word offset splitting.
  - Search by genre and author UUID arrays.
  - Blocked story exclusion for child search.
  - Guardian cannot block an unaccepted child.
