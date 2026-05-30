package com.lexease.notifications.dtos.res;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ReminderResponse(
        UUID scheduleId,
        UUID childId,
        UUID guardianId,
        List<String> daysOfWeek,
        LocalTime time,
        String timezone,
        String message,
        boolean enabled,
        Instant nextRunAt,
        Instant createdAt,
        Instant updatedAt
) {
}
