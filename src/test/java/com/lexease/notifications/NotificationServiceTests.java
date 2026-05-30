package com.lexease.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.notifications.dtos.req.CreateReminderRequest;
import com.lexease.notifications.dtos.req.NotificationStatusRequest;
import com.lexease.notifications.dtos.req.RegisterDeviceTokenRequest;
import com.lexease.notifications.dtos.res.NotificationEventResponse;
import com.lexease.notifications.dtos.res.ReminderResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private ReminderService reminderService;
    @Autowired
    private DeviceTokenService deviceTokenService;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private NotificationSchedulerService schedulerService;
    @Autowired
    private ReminderScheduleRepository reminderScheduleRepository;
    @Autowired
    private NotificationEventRepository notificationEventRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianChildLinkRepository guardianChildLinkRepository;
    @Autowired
    private TestPushSender testPushSender;
    @Autowired
    private EntityManager entityManager;

    @Test
    void acceptedGuardianCanCreatePatchAndDeleteReminder() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-notifications@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-notifications@example.com");
        saveAcceptedLink(guardian, child);

        ReminderResponse created = reminderService.create(
                guardian.getId(),
                UserRole.GUARDIAN,
                createReminder(child.getId()));

        assertThat(created.childId()).isEqualTo(child.getId());
        assertThat(created.enabled()).isTrue();
        assertThat(created.nextRunAt()).isNotNull();

        ReminderResponse patched = reminderService.patch(
                guardian.getId(),
                UserRole.GUARDIAN,
                created.scheduleId(),
                new com.lexease.notifications.dtos.req.PatchReminderRequest(
                        List.of(DayOfWeek.TUESDAY),
                        LocalTime.of(20, 0),
                        "Asia/Ho_Chi_Minh",
                        "Doc tiep nao!",
                        true));

        assertThat(patched.daysOfWeek()).containsExactly("TUESDAY");
        assertThat(patched.message()).isEqualTo("Doc tiep nao!");

        reminderService.delete(guardian.getId(), UserRole.GUARDIAN, created.scheduleId());
        ReminderSchedule schedule = reminderScheduleRepository.findById(created.scheduleId()).orElseThrow();
        assertThat(schedule.isEnabled()).isFalse();
        assertThat(schedule.getNextRunAt()).isNull();
    }

    @Test
    void guardianWithoutAcceptedLinkCannotCreateReminder() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-notifications-2@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-notifications-2@example.com");

        assertThatThrownBy(() -> reminderService.create(
                guardian.getId(),
                UserRole.GUARDIAN,
                createReminder(child.getId())))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot manage reminders for child");
    }

    @Test
    void deviceTokenOwnerCanRegisterAndDeactivateToken() {
        UserAccount child = saveUser(UserRole.CHILD, "child-token@example.com");

        var response = deviceTokenService.register(
                child.getId(),
                new RegisterDeviceTokenRequest(DevicePlatform.ANDROID, "fcm-token", "device-1"));

        assertThat(response.userId()).isEqualTo(child.getId());
        assertThat(response.active()).isTrue();

        deviceTokenService.deactivate(child.getId(), UserRole.CHILD, response.id());

        DeviceToken token = deviceTokenRepository.findById(response.id()).orElseThrow();
        assertThat(token.isActive()).isFalse();
    }

    @Test
    void schedulerCreatesEventDispatchesPayloadAndDoesNotDuplicateScheduleRun() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-scheduler@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-scheduler@example.com");
        saveAcceptedLink(guardian, child);
        deviceTokenService.register(
                child.getId(),
                new RegisterDeviceTokenRequest(DevicePlatform.ANDROID, "fcm-token-scheduler", "device-scheduler"));
        ReminderSchedule schedule = saveDueSchedule(guardian, child);

        int planned = schedulerService.planDueReminders();
        int plannedAgain = schedulerService.planDueReminders();
        int dispatched = schedulerService.dispatchDueEvents();

        List<NotificationEvent> events = notificationEventRepository.findAll();
        assertThat(planned).isEqualTo(1);
        assertThat(plannedAgain).isEqualTo(0);
        assertThat(dispatched).isEqualTo(1);
        assertThat(events).hasSize(1);
        NotificationEvent event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(event.getDeepLink()).isEqualTo("lexease://reading/practice?childId=" + child.getId());
        assertThat(testPushSender.getLastUserId()).isEqualTo(child.getId());
        assertThat(testPushSender.getLastData())
                .containsEntry("type", "PRACTICE_REMINDER")
                .containsEntry("deepLink", event.getDeepLink())
                .containsEntry("notificationEventId", event.getId().toString());
        assertThat(schedule.getNextRunAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    void invalidPushTokenIsDeactivatedAndEventFails() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-invalid-token@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-invalid-token@example.com");
        saveAcceptedLink(guardian, child);
        var token = deviceTokenService.register(
                child.getId(),
                new RegisterDeviceTokenRequest(DevicePlatform.IOS, "fcm-token-invalid", "device-invalid"));
        saveDueSchedule(guardian, child);

        schedulerService.planDueReminders();
        testPushSender.invalidateNextSendTokens();
        schedulerService.dispatchDueEvents();

        NotificationEvent event = notificationEventRepository.findAll().getFirst();
        assertThat(event.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(deviceTokenRepository.findById(token.id()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void statusReportTracksOpenedAndPracticeStarted() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-status@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-status@example.com");
        saveAcceptedLink(guardian, child);
        NotificationEvent event = notificationEventRepository.save(new NotificationEvent(
                UUID.randomUUID(),
                null,
                child,
                "lexease://reading/practice?childId=" + child.getId(),
                Instant.parse("2026-05-25T12:30:00Z"),
                NOW));
        event.markSent(Instant.parse("2026-05-25T12:30:05Z"));

        NotificationEventResponse opened = notificationEventService.reportStatus(
                child.getId(),
                UserRole.CHILD,
                event.getId(),
                new NotificationStatusRequest(
                        NotificationStatus.OPENED_LATE,
                        OffsetDateTime.parse("2026-05-25T19:40:00+07:00")));
        NotificationEventResponse practiced = notificationEventService.reportStatus(
                child.getId(),
                UserRole.CHILD,
                event.getId(),
                new NotificationStatusRequest(
                        NotificationStatus.PRACTICE_STARTED,
                        OffsetDateTime.parse("2026-05-25T19:45:00+07:00")));

        assertThat(opened.status()).isEqualTo(NotificationStatus.OPENED_ON_TIME);
        assertThat(practiced.status()).isEqualTo(NotificationStatus.PRACTICE_STARTED);
        assertThat(practiced.practiceStartedAt()).isEqualTo(Instant.parse("2026-05-25T12:45:00Z"));
    }

    @Test
    void ignoredJobMarksSentEventsAfterThreshold() {
        UserAccount child = saveUser(UserRole.CHILD, "child-ignored@example.com");
        NotificationEvent event = notificationEventRepository.save(new NotificationEvent(
                UUID.randomUUID(),
                null,
                child,
                "lexease://reading/practice?childId=" + child.getId(),
                Instant.now().minusSeconds(90_000),
                Instant.now().minusSeconds(90_000)));
        event.markSent(Instant.now().minusSeconds(90_000));
        entityManager.flush();
        entityManager.clear();

        int ignored = schedulerService.markIgnoredEvents();
        entityManager.clear();

        assertThat(ignored).isEqualTo(1);
        assertThat(notificationEventRepository.findById(event.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationStatus.IGNORED);
    }

    private CreateReminderRequest createReminder(UUID childId) {
        return new CreateReminderRequest(
                childId,
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(19, 30),
                "Asia/Ho_Chi_Minh",
                "Den gio luyen doc roi con nhe!");
    }

    private ReminderSchedule saveDueSchedule(UserAccount guardian, UserAccount child) {
        return reminderScheduleRepository.save(new ReminderSchedule(
                UUID.randomUUID(),
                guardian,
                child,
                List.of("MONDAY", "WEDNESDAY", "FRIDAY"),
                LocalTime.of(19, 30),
                "Asia/Ho_Chi_Minh",
                "Den gio luyen doc roi con nhe!",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60)));
    }

    private UserAccount saveUser(UserRole role, String email) {
        return userRepository.save(new UserAccount(
                UUID.randomUUID(),
                email,
                "hash",
                email,
                role,
                UserStatus.ACTIVE,
                NOW,
                NOW));
    }

    private void saveAcceptedLink(UserAccount guardian, UserAccount child) {
        GuardianChildLink link = new GuardianChildLink(
                UUID.randomUUID(),
                guardian,
                child,
                GuardianChildLinkStatus.PENDING,
                guardian,
                NOW);
        link.accept(NOW);
        guardianChildLinkRepository.save(link);
    }
}
