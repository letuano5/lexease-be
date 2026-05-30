package com.lexease.notifications;

import com.lexease.notifications.dtos.req.NotificationStatusRequest;
import com.lexease.notifications.dtos.res.NotificationEventResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationEventService notificationEventService;

    public NotificationController(NotificationEventService notificationEventService) {
        this.notificationEventService = notificationEventService;
    }

    @PostMapping("/{id}/status")
    NotificationEventResponse reportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody NotificationStatusRequest request
    ) {
        return notificationEventService.reportStatus(principal.id(), principal.role(), id, request);
    }
}
