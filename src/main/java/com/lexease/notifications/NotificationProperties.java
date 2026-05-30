package com.lexease.notifications;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lexease.notifications")
public record NotificationProperties(
        @Valid Firebase firebase,
        @NotNull Duration openedOnTimeWindow,
        @NotNull Duration ignoredAfter,
        @Valid Scheduler scheduler
) {
    public record Firebase(
            String projectId,
            String credentialsPath
    ) {
    }

    public record Scheduler(
            boolean enabled
    ) {
    }
}
