# Phase 4 - Push And Notification Tracking

Mục tiêu: guardian tạo lịch nhắc luyện đọc; backend gửi push đúng giờ; app report trạng thái mở/đọc/thực hiện/bỏ qua.

Lưu ý: backend tự host scheduler, DB, trạng thái và deep link. Nhưng mobile remote push khi app background/killed vẫn cần delivery qua FCM/APNs.

## Input

Register device token:

```json
{
  "platform": "ANDROID",
  "deviceToken": "fcm-token",
  "deviceId": "device-uuid"
}
```

Create reminder:

```json
{
  "childId": "uuid",
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "time": "19:30",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Den gio luyen doc roi con nhe!"
}
```

Track notification status:

```json
{
  "notificationEventId": "uuid",
  "status": "OPENED_LATE",
  "occurredAt": "2026-05-25T19:45:00+07:00"
}
```

## Output

Create reminder:

```json
{
  "scheduleId": "uuid",
  "childId": "uuid",
  "enabled": true,
  "nextRunAt": "2026-05-27T12:30:00Z"
}
```

Notification event:

```json
{
  "notificationEventId": "uuid",
  "status": "SENT",
  "deepLink": "lexease://reading/practice?childId=uuid"
}
```

## Data Model

`device_tokens`

- `id uuid primary key`
- `user_id uuid not null references users(id)`
- `platform text not null check in ('IOS','ANDROID')`
- `device_id text null`
- `token text not null`
- `active boolean not null default true`
- `last_seen_at timestamptz not null`
- `created_at timestamptz not null`

`reminder_schedules`

- `id uuid primary key`
- `guardian_id uuid not null references users(id)`
- `child_id uuid not null references users(id)`
- `days_of_week text[] not null`
- `local_time time not null`
- `timezone text not null`
- `message text not null`
- `enabled boolean not null default true`
- `next_run_at timestamptz null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`notification_events`

- `id uuid primary key`
- `schedule_id uuid null references reminder_schedules(id)`
- `child_id uuid not null references users(id)`
- `status text not null`
- `deep_link text not null`
- `scheduled_for timestamptz not null`
- `sent_at timestamptz null`
- `opened_at timestamptz null`
- `practice_started_at timestamptz null`
- `failure_reason text null`
- `metadata jsonb not null default '{}'`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

Status:

- `SCHEDULED`
- `SENT`
- `FAILED`
- `OPENED_ON_TIME`
- `OPENED_LATE`
- `PRACTICE_STARTED`
- `IGNORED`

## Implementation

### 4.1 Device token management

- Child app registers FCM/APNs token after login.
- Token is associated with child user.
- Deactivate token when push provider says token invalid.

### 4.2 Reminder schedule

- Guardian can create reminder only for accepted child.
- Store local time + timezone; compute `next_run_at` in UTC.
- Scheduler scans due schedules every minute.
- Use DB lock/claim pattern to avoid double send if multiple app instances later.

### 4.3 Push sender

Interface:

```text
PushSender.send(userId, title, body, data)
```

MVP provider:

- FCM for Android.
- For iOS, decide whether to use FCM bridge or direct APNs.

Decision needed: delivery provider for iOS.

Payload data:

```json
{
  "type": "PRACTICE_REMINDER",
  "deepLink": "lexease://reading/practice?childId=uuid",
  "notificationEventId": "uuid"
}
```

### 4.4 Tracking

- Backend creates `notification_events` before sending.
- App reports when opened and when practice starts.
- `DELIVERED` is not guaranteed from provider; do not rely on it as source of truth.
- Backend marks `IGNORED` by scheduled job if no open/practice after threshold.

Decision needed: threshold for `OPENED_ON_TIME` vs `OPENED_LATE`.

## APIs

- `POST /device-tokens`
- `DELETE /device-tokens/{id}`
- `POST /reminders`
- `GET /children/{childId}/reminders`
- `PATCH /reminders/{id}`
- `DELETE /reminders/{id}`
- `POST /notifications/{id}/status`

## Done Criteria

- Guardian accepted child tạo/sửa/xoá reminder được.
- Scheduler tạo event và gọi push sender.
- Deep link được lưu trong notification event.
- App status report cập nhật event.
- Token invalid được deactivate.
- Có test permission và next-run calculation theo timezone.

