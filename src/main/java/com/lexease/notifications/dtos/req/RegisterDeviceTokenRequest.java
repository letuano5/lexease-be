package com.lexease.notifications.dtos.req;

import com.lexease.notifications.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenRequest(
        @NotNull DevicePlatform platform,
        @NotBlank String deviceToken,
        String deviceId
) {
}
