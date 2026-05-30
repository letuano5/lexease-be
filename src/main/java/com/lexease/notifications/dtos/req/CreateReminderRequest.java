package com.lexease.notifications.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateReminderRequest(
        @NotNull UUID childId,
        @NotEmpty List<DayOfWeek> daysOfWeek,
        @NotNull LocalTime time,
        @NotBlank String timezone,
        @NotBlank @Size(max = 500) String message
) {
}
