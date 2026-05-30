package com.lexease.display;

import com.lexease.display.dtos.req.SaveDisplaySettingsRequest;
import com.lexease.display.dtos.res.DisplaySettingsResponse;
import com.lexease.guardians.PermissionService;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.audit.AuditAction;
import com.lexease.shared.audit.AuditService;
import com.lexease.shared.audit.AuditTargetType;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisplaySettingsService {
    private final DisplaySettingsRepository displaySettingsRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final Clock clock;
    private final DisplaySettingsDefaults defaults = DisplaySettingsDefaults.standard();

    public DisplaySettingsService(
            DisplaySettingsRepository displaySettingsRepository,
            UserRepository userRepository,
            PermissionService permissionService,
            AuditService auditService,
            Clock clock
    ) {
        this.displaySettingsRepository = displaySettingsRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DisplaySettingsResponse get(UUID actorId, UserRole actorRole, UUID childId) {
        requireAccess(actorId, actorRole, childId, true);
        return displaySettingsRepository.findByChildId(childId)
                .map(DisplaySettingsResponse::from)
                .orElseGet(() -> DisplaySettingsResponse.defaults(childId, defaults));
    }

    @Transactional
    public DisplaySettingsResponse save(
            UUID actorId,
            UserRole actorRole,
            UUID childId,
            SaveDisplaySettingsRequest request
    ) {
        UserAccount child = requireAccess(actorId, actorRole, childId, false);
        Instant now = Instant.now(clock);
        Optional<DisplaySettings> existingSettings = displaySettingsRepository.findByChildId(childId);
        DisplaySettings settings = existingSettings
                .orElseGet(() -> new DisplaySettings(UUID.randomUUID(), child, defaults, now, now));
        settings.save(
                request.fontFamily().trim(),
                request.fontSize(),
                request.lineHeight(),
                request.letterSpacing(),
                normalizeHex(request.backgroundColor()),
                normalizeHex(request.textColor()),
                normalizeHex(request.highlightBackgroundColor()),
                normalizeHex(request.highlightTextColor()),
                trimToNull(request.themeName()),
                now,
                existingSettings.isPresent());
        settings = displaySettingsRepository.save(settings);
        auditService.log(actorId, AuditAction.DISPLAY_SETTINGS_SAVED, AuditTargetType.DISPLAY_SETTINGS, settings.getId());
        return DisplaySettingsResponse.from(settings);
    }

    @Transactional
    public DisplaySettingsResponse reset(UUID actorId, UserRole actorRole, UUID childId) {
        UserAccount child = requireAccess(actorId, actorRole, childId, false);
        Instant now = Instant.now(clock);
        DisplaySettings settings = displaySettingsRepository.findByChildId(childId)
                .orElse(null);
        if (settings == null) {
            settings = new DisplaySettings(UUID.randomUUID(), child, defaults, now, now);
        } else {
            settings.reset(defaults, now);
        }
        settings = displaySettingsRepository.save(settings);
        auditService.log(actorId, AuditAction.DISPLAY_SETTINGS_RESET, AuditTargetType.DISPLAY_SETTINGS, settings.getId());
        return DisplaySettingsResponse.from(settings);
    }

    private UserAccount requireAccess(UUID actorId, UserRole actorRole, UUID childId, boolean readOnly) {
        UserAccount child = findChild(childId);
        boolean allowed = readOnly
                ? permissionService.canAccessChild(actorId, actorRole, childId)
                : permissionService.canManageDisplaySettings(actorId, actorRole, childId);
        if (!allowed) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.DISPLAY_SETTINGS_FORBIDDEN,
                    "Cannot access display settings for child");
        }
        return child;
    }

    private UserAccount findChild(UUID childId) {
        UserAccount child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, "Child not found"));
        if (child.getRole() != UserRole.CHILD || child.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_CHILD, "Invalid child");
        }
        return child;
    }

    private String normalizeHex(String value) {
        return value.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
