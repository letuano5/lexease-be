package com.lexease.notifications.dtos.req;

import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record PatchReminderRequest(
        List<DayOfWeek> daysOfWeek,
        LocalTime time,
        String timezone,
        @Size(max = 500) String message,
        Boolean enabled
) {
}
