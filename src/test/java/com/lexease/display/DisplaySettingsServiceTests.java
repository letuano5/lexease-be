package com.lexease.display;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lexease.display.dtos.req.SaveDisplaySettingsRequest;
import com.lexease.display.dtos.res.DisplaySettingsResponse;
import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.shared.api.ApiException;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisplaySettingsServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private DisplaySettingsService displaySettingsService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianChildLinkRepository guardianChildLinkRepository;

    @Test
    void childCanSaveOwnSettings() {
        UserAccount child = saveUser(UserRole.CHILD, "child-display@example.com");

        DisplaySettingsResponse response = displaySettingsService.save(
                child.getId(),
                UserRole.CHILD,
                child.getId(),
                request());

        assertThat(response.childId()).isEqualTo(child.getId());
        assertThat(response.fontFamily()).isEqualTo("OpenDyslexic");
        assertThat(response.highlightBackgroundColor()).isEqualTo("#FDE68A");
        assertThat(response.highlightTextColor()).isEqualTo("#1F2937");
        assertThat(response.settingsVersion()).isEqualTo(1);
    }

    @Test
    void guardianWithoutAcceptedLinkCannotSaveSettings() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-display@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-display-2@example.com");

        assertThatThrownBy(() -> displaySettingsService.save(
                guardian.getId(),
                UserRole.GUARDIAN,
                child.getId(),
                request()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot access display settings for child");
    }

    @Test
    void acceptedGuardianCanResetAndIncrementExistingSettingsVersion() {
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-display-2@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child-display-3@example.com");
        saveAcceptedLink(guardian, child);
        displaySettingsService.save(guardian.getId(), UserRole.GUARDIAN, child.getId(), request());

        DisplaySettingsResponse response = displaySettingsService.reset(
                guardian.getId(),
                UserRole.GUARDIAN,
                child.getId());

        assertThat(response.fontFamily()).isEqualTo("system");
        assertThat(response.highlightBackgroundColor()).isEqualTo("#FEF08A");
        assertThat(response.highlightTextColor()).isEqualTo("#111111");
        assertThat(response.settingsVersion()).isEqualTo(2);
    }

    private SaveDisplaySettingsRequest request() {
        return new SaveDisplaySettingsRequest(
                "OpenDyslexic",
                24,
                new BigDecimal("1.90"),
                new BigDecimal("0.10"),
                "#fff7d6",
                "#111827",
                "#fde68a",
                "#1f2937",
                "warm-low-glare");
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
