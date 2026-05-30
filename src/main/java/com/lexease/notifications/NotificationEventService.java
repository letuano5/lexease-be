package com.lexease.notifications;

import com.lexease.guardians.PermissionService;
import com.lexease.notifications.dtos.req.NotificationStatusRequest;
import com.lexease.notifications.dtos.res.NotificationEventResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.users.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventService {
    private final NotificationEventRepository notificationEventRepository;
    private final PermissionService permissionService;
    private final NotificationProperties properties;
    private final Clock clock;

    public NotificationEventService(
            NotificationEventRepository notificationEventRepository,
            PermissionService permissionService,
            NotificationProperties properties,
            Clock clock
    ) {
        this.notificationEventRepository = notificationEventRepository;
        this.permissionService = permissionService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public NotificationEventResponse reportStatus(UUID currentUserId, UserRole currentRole, UUID eventId, NotificationStatusRequest request) {
        NotificationEvent event = notificationEventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOTIFICATION_EVENT_NOT_FOUND, "Notification event not found"));
        if (!permissionService.canAccessChild(currentUserId, currentRole, event.getChild().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOTIFICATION_EVENT_FORBIDDEN, "Cannot update notification event");
        }
        Instant occurredAt = request.occurredAt().toInstant();
        Instant now = Instant.now(clock);
        if (request.status() == NotificationStatus.PRACTICE_STARTED) {
            event.markPracticeStarted(occurredAt, now);
            return toResponse(event);
        }
        if (request.status() != NotificationStatus.OPENED_ON_TIME && request.status() != NotificationStatus.OPENED_LATE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_NOTIFICATION_STATUS, "Unsupported notification status report");
        }
        if (event.getStatus() != NotificationStatus.PRACTICE_STARTED) {
            event.markOpened(openedStatus(event, occurredAt), occurredAt, now);
        }
        return toResponse(event);
    }

    NotificationEventResponse toResponse(NotificationEvent event) {
        return new NotificationEventResponse(
                event.getId(),
                event.getStatus(),
                event.getDeepLink(),
                event.getScheduledFor(),
                event.getSentAt(),
                event.getOpenedAt(),
                event.getPracticeStartedAt(),
                event.getFailureReason());
    }

    private NotificationStatus openedStatus(NotificationEvent event, Instant occurredAt) {
        Instant onTimeCutoff = event.getScheduledFor().plus(properties.openedOnTimeWindow());
        return occurredAt.compareTo(onTimeCutoff) <= 0
                ? NotificationStatus.OPENED_ON_TIME
                : NotificationStatus.OPENED_LATE;
    }
}
