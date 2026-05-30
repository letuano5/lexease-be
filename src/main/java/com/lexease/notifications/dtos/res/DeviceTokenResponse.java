package com.lexease.notifications.dtos.res;

import com.lexease.notifications.DevicePlatform;
import java.time.Instant;
import java.util.UUID;

public record DeviceTokenResponse(
        UUID id,
        UUID userId,
        DevicePlatform platform,
        String deviceId,
        boolean active,
        Instant lastSeenAt,
        Instant createdAt
) {
}
