# Phase 4 - Notifications Process

## Implemented Features

- Flyway schema for device tokens, reminder schedules, and notification events in `V9__init_notifications.sql`.
- Device token register/deactivate APIs.
- Guardian reminder create/list/update/delete APIs.
- Reminder permission checks through guardian-child relation.
- Next-run calculation using the reminder local time and IANA timezone.
- Scheduler jobs for:
  - planning due reminders into notification events,
  - dispatching due notification events through push,
  - marking old sent events as `IGNORED`.
- Firebase Cloud Messaging sender for non-test profiles.
- Test push sender for the `test` profile.
- Deep link payload for reading practice.
- Notification status tracking for open and practice-start events.
- Invalid Firebase tokens are deactivated when provider reports them as invalid.
- Notification config is type-bound with `@ConfigurationProperties`.

## Data Model

Implemented in `V9__init_notifications.sql`.

`device_tokens`

- `id`: UUID primary key.
- `user_id`: token owner. For reminder delivery this should be the child user.
- `platform`: `IOS` or `ANDROID`.
- `device_id`: optional client device id.
- `token`: Firebase registration token.
- `active`: whether this token can receive push.
- `last_seen_at`: refreshed whenever the token is registered again.
- `created_at`: creation timestamp.
- Unique constraints:
  - token value is globally unique.
  - active `(user_id, platform, device_id)` is unique when `device_id` is present.

`reminder_schedules`

- `id`: UUID primary key.
- `guardian_id`: guardian who created the schedule.
- `child_id`: child to notify.
- `days_of_week`: text array of Java `DayOfWeek` names.
- `local_time`: reminder local time.
- `timezone`: IANA timezone, for example `Asia/Ho_Chi_Minh`.
- `message`: notification body.
- `enabled`: disabled schedules are not planned.
- `next_run_at`: next UTC instant to plan.
- `created_at`, `updated_at`: audit timestamps.

`notification_events`

- `id`: UUID primary key.
- `schedule_id`: source reminder schedule, nullable.
- `child_id`: child receiving the notification.
- `status`: `SCHEDULED`, `SENT`, `FAILED`, `OPENED_ON_TIME`, `OPENED_LATE`, `PRACTICE_STARTED`, or `IGNORED`.
- `deep_link`: app deep link to open reading practice.
- `scheduled_for`: UTC instant the reminder was scheduled for.
- `sent_at`: set when at least one token is sent successfully.
- `opened_at`: set by app status report.
- `practice_started_at`: set by app status report.
- `failure_reason`: provider/backend send failure reason.
- `metadata`: reserved JSONB payload.
- `created_at`, `updated_at`: audit timestamps.
- Unique index prevents duplicate event creation for the same `(schedule_id, scheduled_for)`.

## Config

Notification config under `lexease.notifications`:

- `FIREBASE_PROJECT_ID`: Firebase project id. Required for non-test profiles.
- `FIREBASE_CREDENTIALS_PATH`: local path to a Firebase service account JSON file. Required for non-test profiles.
- `NOTIFICATIONS_OPENED_ON_TIME_WINDOW`: window after `scheduled_for` that still counts as `OPENED_ON_TIME`. Default: `15m`.
- `NOTIFICATIONS_IGNORED_AFTER`: how long a sent event can stay unopened before the ignored job marks it `IGNORED`. Default: `24h`.
- `NOTIFICATIONS_SCHEDULER_ENABLED`: enables/disables all notification scheduled jobs. Default: `true`.

Important local/prod setup:

- Firebase config is created in all non-test profiles. The app will fail startup if `FIREBASE_PROJECT_ID` or `FIREBASE_CREDENTIALS_PATH` is blank.
- The service account JSON should not be committed. Store it outside git and pass its path through env/config.
- Mobile should register Firebase registration tokens. iOS is currently supported through Firebase Messaging/APNs bridge, not direct APNs.
- Multiple active tokens for the same child are all sent to.

Example local env:

```bash
export FIREBASE_PROJECT_ID=lexease-dev
export FIREBASE_CREDENTIALS_PATH=/absolute/path/to/firebase-service-account.json
export NOTIFICATIONS_SCHEDULER_ENABLED=true
export NOTIFICATIONS_OPENED_ON_TIME_WINDOW=15m
export NOTIFICATIONS_IGNORED_AFTER=24h
```

To run backend without dispatching scheduler jobs but still using a non-test profile:

```bash
export NOTIFICATIONS_SCHEDULER_ENABLED=false
```

Note: disabling the scheduler does not skip Firebase bean initialization in the current implementation.

## Firebase Setup

1. Create or select a Firebase project for the environment.
2. Add the mobile apps to the Firebase project.
3. For Android, configure the app with Firebase and obtain FCM registration tokens from the client SDK.
4. For iOS, configure APNs key/certificate in Firebase, enable Firebase Messaging in the app, and register the Firebase token with backend.
5. In Firebase Console, create a service account private key JSON for the backend.
6. Put the JSON file on the backend host outside the repository.
7. Set `FIREBASE_PROJECT_ID` and `FIREBASE_CREDENTIALS_PATH`.
8. Ensure the backend process can read the JSON file.

## Endpoint: `POST /device-tokens`

Registers or refreshes the current user's device token.

Input:

```json
{
  "platform": "ANDROID",
  "deviceToken": "fcm-token",
  "deviceId": "device-uuid"
}
```

Input fields:

- `platform`: required, `IOS` or `ANDROID`.
- `deviceToken`: required Firebase registration token.
- `deviceId`: optional stable client device id.

Output:

```json
{
  "id": "uuid",
  "userId": "uuid",
  "platform": "ANDROID",
  "deviceId": "device-uuid",
  "active": true,
  "lastSeenAt": "2026-05-25T12:00:00Z",
  "createdAt": "2026-05-25T12:00:00Z"
}
```

Implementation:

- `DeviceTokenController` delegates to `DeviceTokenService.register`.
- Existing token value is reused and refreshed.
- Token is associated with the authenticated user.
- For reminders, child app should call this endpoint after login and whenever Firebase rotates the token.

## Endpoint: `DELETE /device-tokens/{id}`

Deactivates a device token.

Input:

- Path `id`: device token UUID.

Output: HTTP `200 OK` with an empty body.

Implementation:

- Token owner can deactivate their token.
- Admin can deactivate any token.
- Other users are rejected with `FORBIDDEN`.
- Token row is retained with `active = false`.

## Endpoint: `POST /reminders`

Creates a reminder schedule.

Input:

```json
{
  "childId": "uuid",
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "time": "19:30",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Den gio luyen doc roi con nhe!"
}
```

Input fields:

- `childId`: required child user UUID.
- `daysOfWeek`: required non-empty list of Java `DayOfWeek` names.
- `time`: required local time.
- `timezone`: required IANA timezone string.
- `message`: required, max 500 chars.

Output:

```json
{
  "scheduleId": "uuid",
  "childId": "uuid",
  "guardianId": "uuid",
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "time": "19:30:00",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Den gio luyen doc roi con nhe!",
  "enabled": true,
  "nextRunAt": "2026-05-27T12:30:00Z",
  "createdAt": "2026-05-25T12:00:00Z",
  "updatedAt": "2026-05-25T12:00:00Z"
}
```

Implementation:

- Uses `PermissionService.canManageChild`.
- Guardian must have an accepted guardian-child link, unless current user is admin.
- `daysOfWeek` is de-duplicated and sorted.
- `nextRunAt` is stored as UTC based on local `time` and `timezone`.

## Endpoint: `GET /children/{childId}/reminders`

Lists reminder schedules for a child.

Input:

- Path `childId`: child UUID.

Output: list of `ReminderResponse`, ordered by local time.

Implementation:

- Uses `PermissionService.canAccessChild`.
- Child can access own reminders.
- Accepted guardian can access linked child reminders.
- Admin can access any child reminders.

## Endpoint: `PATCH /reminders/{id}`

Updates a reminder schedule.

Input:

```json
{
  "daysOfWeek": ["TUESDAY"],
  "time": "20:00",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Doc tiep nao!",
  "enabled": true
}
```

Input fields:

- All fields are optional.
- If `enabled` becomes `false`, `nextRunAt` is cleared.
- If `enabled` is `true`, `nextRunAt` is recalculated from the updated schedule.

Output: `ReminderResponse`.

Implementation:

- Uses `PermissionService.canManageChild`.
- Message is trimmed when provided.
- Empty `daysOfWeek`, blank timezone, or blank message are rejected.

## Endpoint: `DELETE /reminders/{id}`

Disables a reminder schedule.

Input:

- Path `id`: reminder UUID.

Output: HTTP `200 OK` with an empty body.

Implementation:

- This is a soft delete: `enabled = false`, `nextRunAt = null`.
- Uses `PermissionService.canManageChild`.

## Scheduler Flow

`NotificationJobs` runs when `NOTIFICATIONS_SCHEDULER_ENABLED=true`.

`planDueReminders`, every 60 seconds:

- Claims up to 100 enabled schedules where `next_run_at <= now`.
- Creates one `notification_events` row per due schedule run.
- Stores deep link as `lexease://reading/practice?childId=<childId>`.
- Advances the reminder schedule to the next run.
- Duplicate creation is prevented by DB unique index and repository checks.

`dispatchDueEvents`, every 60 seconds:

- Claims up to 100 `SCHEDULED` notification events where `scheduled_for <= now`.
- Sends through `PushSender`.
- Firebase payload data:

```json
{
  "type": "PRACTICE_REMINDER",
  "deepLink": "lexease://reading/practice?childId=uuid",
  "notificationEventId": "uuid"
}
```

- Marks event `SENT` if at least one active token is sent successfully.
- Marks event `FAILED` when no token is sent successfully.
- Deactivates invalid tokens returned by the push sender.

`markIgnoredEvents`, every 15 minutes:

- Marks `SENT` events as `IGNORED` when `scheduled_for` is older than `NOTIFICATIONS_IGNORED_AFTER`.

## Endpoint: `POST /notifications/{id}/status`

Reports app-side notification status.

Input:

```json
{
  "status": "OPENED_LATE",
  "occurredAt": "2026-05-25T19:45:00+07:00"
}
```

Supported report statuses:

- `OPENED_ON_TIME`
- `OPENED_LATE`
- `PRACTICE_STARTED`

Output:

```json
{
  "notificationEventId": "uuid",
  "status": "PRACTICE_STARTED",
  "deepLink": "lexease://reading/practice?childId=uuid",
  "scheduledFor": "2026-05-25T12:30:00Z",
  "sentAt": "2026-05-25T12:30:05Z",
  "openedAt": "2026-05-25T12:40:00Z",
  "practiceStartedAt": "2026-05-25T12:45:00Z",
  "failureReason": null
}
```

Implementation:

- Uses `PermissionService.canAccessChild` for the event child.
- Backend computes `OPENED_ON_TIME` vs `OPENED_LATE`; the client-provided open status is treated as an open signal.
- `OPENED_ON_TIME` means `occurredAt <= scheduled_for + NOTIFICATIONS_OPENED_ON_TIME_WINDOW`.
- `PRACTICE_STARTED` updates `practice_started_at` and status to `PRACTICE_STARTED`.
- `DELIVERED` is intentionally not tracked because provider delivery receipts are not reliable as the source of truth for this MVP.

## Manual Test Flow

Prerequisites:

- Database is migrated through `V9__init_notifications.sql`.
- Backend runs in a non-test profile with valid Firebase env vars.
- `NOTIFICATIONS_SCHEDULER_ENABLED=true`.
- Guardian and child users exist.
- Guardian-child link is `ACCEPTED`.
- Child mobile app can obtain a Firebase registration token.

Flow:

1. Login as child and register device token:

```http
POST /device-tokens
Authorization: Bearer <child-access-token>
Content-Type: application/json

{
  "platform": "ANDROID",
  "deviceToken": "<firebase-registration-token>",
  "deviceId": "local-test-device"
}
```

2. Login as guardian and create a reminder a few minutes in the future in the child's timezone:

```http
POST /reminders
Authorization: Bearer <guardian-access-token>
Content-Type: application/json

{
  "childId": "<child-id>",
  "daysOfWeek": ["SATURDAY"],
  "time": "19:30",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Den gio luyen doc roi con nhe!"
}
```

If testing on a different weekday, replace `SATURDAY` with the current local `DayOfWeek`.

3. Confirm response has `enabled=true` and `nextRunAt` near the intended time.
4. Wait for the scheduler to run. It can take up to about 2 minutes because planning and dispatch both run every 60 seconds.
5. Confirm the child device receives a push notification.
6. Open the notification in the app and navigate using `deepLink`.
7. App reports open status using the `notificationEventId` from push payload:

```http
POST /notifications/<notificationEventId>/status
Authorization: Bearer <child-access-token>
Content-Type: application/json

{
  "status": "OPENED_ON_TIME",
  "occurredAt": "2026-05-30T19:31:00+07:00"
}
```

8. When the child starts reading practice, app reports:

```http
POST /notifications/<notificationEventId>/status
Authorization: Bearer <child-access-token>
Content-Type: application/json

{
  "status": "PRACTICE_STARTED",
  "occurredAt": "2026-05-30T19:32:00+07:00"
}
```

9. Check database:

```sql
select id, status, deep_link, scheduled_for, sent_at, opened_at, practice_started_at, failure_reason
from notification_events
order by created_at desc
limit 5;
```

Expected result:

- Event moves `SCHEDULED -> SENT`.
- After open report, event is `OPENED_ON_TIME` or `OPENED_LATE`.
- After practice report, event is `PRACTICE_STARTED`.
- Deep link is `lexease://reading/practice?childId=<child-id>`.

Failure cases to test:

- Guardian without accepted child link cannot create reminder.
- No active device token causes event `FAILED`.
- Invalid Firebase token is deactivated.
- If app does not report open/practice, the ignored job marks sent events `IGNORED` after `NOTIFICATIONS_IGNORED_AFTER`.
- Disable a reminder and confirm `next_run_at` is null and no new event is planned.

## Automated Verification

- `./gradlew compileJava`
- `./gradlew test`

Added tests:

- Accepted guardian can create, patch, and delete reminder.
- Guardian without accepted link cannot create reminder.
- Device token owner can register and deactivate token.
- Scheduler creates event, dispatches push payload, and does not duplicate a schedule run.
- Invalid push token is deactivated and event fails.
- Status report tracks opened and practice-started events.
- Ignored job marks old sent events as `IGNORED`.
- Next-run calculation handles timezone, same-day future time, passed same-day time, and multiple days.

## Decisions

- Push provider: Firebase Cloud Messaging. iOS uses Firebase Messaging/APNs bridge; direct APNs sender is not implemented.
- `OPENED_ON_TIME` default window: 15 minutes after `scheduled_for`, configurable by `NOTIFICATIONS_OPENED_ON_TIME_WINDOW`.
- Multiple devices: send to all active tokens for the child.
- Delete reminder behavior: soft disable, not physical delete.
- Delivery receipt is not part of MVP tracking.

## Known Gaps

- Firebase sender sends one message per token instead of using multicast/batch send.
- There is no admin/debug endpoint to manually trigger scheduler jobs; tests call `NotificationSchedulerService` directly.
- Firebase bean is required in non-test profiles even when scheduler is disabled.
- No audit log entries are written for reminder or token changes yet.
