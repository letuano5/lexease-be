package com.lexease.notifications;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSchedulerService.class);
    private static final int BATCH_SIZE = 100;
    private static final String TITLE = "LexEase";

    private final ReminderScheduleRepository reminderScheduleRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final ReminderNextRunCalculator nextRunCalculator;
    private final PushSender pushSender;
    private final DeviceTokenService deviceTokenService;
    private final NotificationProperties properties;
    private final Clock clock;

    public NotificationSchedulerService(
            ReminderScheduleRepository reminderScheduleRepository,
            NotificationEventRepository notificationEventRepository,
            ReminderNextRunCalculator nextRunCalculator,
            PushSender pushSender,
            DeviceTokenService deviceTokenService,
            NotificationProperties properties,
            Clock clock
    ) {
        this.reminderScheduleRepository = reminderScheduleRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.nextRunCalculator = nextRunCalculator;
        this.pushSender = pushSender;
        this.deviceTokenService = deviceTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int planDueReminders() {
        Instant now = Instant.now(clock);
        List<ReminderSchedule> schedules = reminderScheduleRepository.claimDueSchedules(now, BATCH_SIZE);
        for (ReminderSchedule schedule : schedules) {
            Instant scheduledFor = schedule.getNextRunAt();
            if (scheduledFor != null && !notificationEventRepository.existsByScheduleIdAndScheduledFor(schedule.getId(), scheduledFor)) {
                notificationEventRepository.save(new NotificationEvent(
                        UUID.randomUUID(),
                        schedule,
                        schedule.getChild(),
                        deepLink(schedule),
                        scheduledFor,
                        now));
            }
            schedule.advance(nextRunCalculator.nextRunAt(
                    schedule.getDaysOfWeek().stream().map(DayOfWeek::valueOf).toList(),
                    schedule.getLocalTime(),
                    schedule.getTimezone(),
                    now), now);
        }
        return schedules.size();
    }

    @Transactional
    public int dispatchDueEvents() {
        Instant now = Instant.now(clock);
        List<NotificationEvent> events = notificationEventRepository.claimDueEvents(now, BATCH_SIZE);
        for (NotificationEvent event : events) {
            PushSendResult result = pushSender.send(
                    event.getChild().getId(),
                    TITLE,
                    body(event),
                    Map.of(
                            "type", "PRACTICE_REMINDER",
                            "deepLink", event.getDeepLink(),
                            "notificationEventId", event.getId().toString()));
            deviceTokenService.deactivateInvalidTokens(result.invalidTokenIds());
            if (result.sent() > 0) {
                event.markSent(now);
            } else {
                event.markFailed(result.failureReason() == null ? "Could not send notification" : result.failureReason(), now);
            }
        }
        return events.size();
    }

    @Transactional
    public int markIgnoredEvents() {
        Instant now = Instant.now(clock);
        return notificationEventRepository.markIgnored(now.minus(properties.ignoredAfter()), now);
    }

    private String deepLink(ReminderSchedule schedule) {
        return "lexease://reading/practice?childId=" + schedule.getChild().getId();
    }

    private String body(NotificationEvent event) {
        ReminderSchedule schedule = event.getSchedule();
        return schedule == null ? "Den gio luyen doc roi con nhe!" : schedule.getMessage();
    }
}
