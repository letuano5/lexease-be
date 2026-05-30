package com.lexease.notifications;

import com.lexease.guardians.PermissionService;
import com.lexease.notifications.dtos.req.CreateReminderRequest;
import com.lexease.notifications.dtos.req.PatchReminderRequest;
import com.lexease.notifications.dtos.res.ReminderResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ReminderNextRunCalculator nextRunCalculator;
    private final Clock clock;

    public ReminderService(
            ReminderScheduleRepository reminderScheduleRepository,
            UserRepository userRepository,
            PermissionService permissionService,
            ReminderNextRunCalculator nextRunCalculator,
            Clock clock
    ) {
        this.reminderScheduleRepository = reminderScheduleRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.nextRunCalculator = nextRunCalculator;
        this.clock = clock;
    }

    @Transactional
    public ReminderResponse create(UUID currentUserId, UserRole currentRole, CreateReminderRequest request) {
        requireCanManage(currentUserId, currentRole, request.childId());
        UserAccount guardian = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "User not found"));
        UserAccount child = findChild(request.childId());
        List<DayOfWeek> days = normalizeDays(request.daysOfWeek());
        String timezone = normalizeText(request.timezone(), "Reminder timezone is required");
        String message = normalizeText(request.message(), "Reminder message is required");
        Instant now = Instant.now(clock);
        ReminderSchedule schedule = new ReminderSchedule(
                UUID.randomUUID(),
                guardian,
                child,
                dayNames(days),
                request.time(),
                timezone,
                message,
                nextRunCalculator.nextRunAt(days, request.time(), timezone, now),
                now);
        return toResponse(reminderScheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> listForChild(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (!permissionService.canAccessChild(currentUserId, currentRole, childId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.REMINDER_FORBIDDEN, "Cannot access reminders for child");
        }
        return reminderScheduleRepository.findByChildIdOrderByLocalTime(childId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReminderResponse patch(UUID currentUserId, UserRole currentRole, UUID scheduleId, PatchReminderRequest request) {
        ReminderSchedule schedule = findSchedule(scheduleId);
        requireCanManage(currentUserId, currentRole, schedule.getChild().getId());
        List<DayOfWeek> days = request.daysOfWeek() == null
                ? schedule.getDaysOfWeek().stream().map(DayOfWeek::valueOf).toList()
                : normalizeDays(request.daysOfWeek());
        LocalTime localTime = request.time() == null ? schedule.getLocalTime() : request.time();
        String timezone = request.timezone() == null
                ? schedule.getTimezone()
                : normalizeText(request.timezone(), "Reminder timezone is required");
        String message = request.message() == null
                ? schedule.getMessage()
                : normalizeText(request.message(), "Reminder message is required");
        boolean enabled = request.enabled() == null ? schedule.isEnabled() : request.enabled();
        Instant now = Instant.now(clock);
        Instant nextRunAt = enabled ? nextRunCalculator.nextRunAt(days, localTime, timezone, now) : null;
        schedule.update(dayNames(days), localTime, timezone, message, enabled, nextRunAt, now);
        return toResponse(schedule);
    }

    @Transactional
    public void delete(UUID currentUserId, UserRole currentRole, UUID scheduleId) {
        ReminderSchedule schedule = findSchedule(scheduleId);
        requireCanManage(currentUserId, currentRole, schedule.getChild().getId());
        schedule.disable(Instant.now(clock));
    }

    ReminderResponse toResponse(ReminderSchedule schedule) {
        return new ReminderResponse(
                schedule.getId(),
                schedule.getChild().getId(),
                schedule.getGuardian().getId(),
                schedule.getDaysOfWeek(),
                schedule.getLocalTime(),
                schedule.getTimezone(),
                schedule.getMessage(),
                schedule.isEnabled(),
                schedule.getNextRunAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }

    private ReminderSchedule findSchedule(UUID scheduleId) {
        return reminderScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.REMINDER_NOT_FOUND, "Reminder not found"));
    }

    private UserAccount findChild(UUID childId) {
        UserAccount child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, "Child not found"));
        if (child.getRole() != UserRole.CHILD) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_CHILD, "User is not a child");
        }
        return child;
    }

    private void requireCanManage(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (!permissionService.canManageChild(currentUserId, currentRole, childId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.REMINDER_FORBIDDEN, "Cannot manage reminders for child");
        }
    }

    private List<DayOfWeek> normalizeDays(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REMINDER, "Reminder days of week cannot be empty");
        }
        return days.stream()
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();
    }

    private List<String> dayNames(List<DayOfWeek> days) {
        return days.stream().map(DayOfWeek::name).toList();
    }

    private String normalizeText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REMINDER, message);
        }
        return value.trim();
    }
}
