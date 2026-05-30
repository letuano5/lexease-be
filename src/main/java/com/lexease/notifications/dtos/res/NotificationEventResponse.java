package com.lexease.notifications.dtos.res;

import com.lexease.notifications.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationEventResponse(
        UUID notificationEventId,
        NotificationStatus status,
        String deepLink,
        Instant scheduledFor,
        Instant sentAt,
        Instant openedAt,
        Instant practiceStartedAt,
        String failureReason
) {
}
