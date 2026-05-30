package com.lexease.notifications;

import com.lexease.notifications.dtos.req.RegisterDeviceTokenRequest;
import com.lexease.notifications.dtos.res.DeviceTokenResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device-tokens")
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    DeviceTokenResponse register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        return deviceTokenService.register(principal.id(), request);
    }

    @DeleteMapping("/{id}")
    void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        deviceTokenService.deactivate(principal.id(), principal.role(), id);
    }
}
