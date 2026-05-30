package com.lexease.notifications.dtos.req;

import com.lexease.notifications.NotificationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record NotificationStatusRequest(
        @NotNull NotificationStatus status,
        @NotNull OffsetDateTime occurredAt
) {
}
