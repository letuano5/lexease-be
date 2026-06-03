package com.lexease.guardians;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.lexease.shared.audit.AuditService;
import com.lexease.shared.security.UserPrincipal;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuardianChildLinkServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Mock
    private GuardianChildLinkRepository linkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    @Test
    void guardianListReturnsAcceptedChildrenItManages() {
        UserAccount guardian = user(UserRole.GUARDIAN, "guardian@example.com");
        UserAccount firstChild = user(UserRole.CHILD, "child-1@example.com");
        UserAccount secondChild = user(UserRole.CHILD, "child-2@example.com");
        GuardianChildLink firstLink = acceptedLink(guardian, firstChild);
        GuardianChildLink secondLink = acceptedLink(guardian, secondChild);
        GuardianChildLinkService service = service();

        when(linkRepository.findByGuardianIdAndStatusOrderByCreatedAtDesc(
                guardian.getId(),
                GuardianChildLinkStatus.ACCEPTED))
                .thenReturn(List.of(firstLink, secondLink));

        var response = service.list(new UserPrincipal(guardian));

        assertThat(response)
                .extracting(link -> link.child().id())
                .containsExactly(firstChild.getId(), secondChild.getId());
        verify(linkRepository).findByGuardianIdAndStatusOrderByCreatedAtDesc(
                guardian.getId(),
                GuardianChildLinkStatus.ACCEPTED);
        verifyNoMoreInteractions(linkRepository);
    }

    private GuardianChildLinkService service() {
        return new GuardianChildLinkService(
                linkRepository,
                userRepository,
                auditService,
                Clock.systemUTC());
    }

    private GuardianChildLink acceptedLink(UserAccount guardian, UserAccount child) {
        GuardianChildLink link = new GuardianChildLink(
                UUID.randomUUID(),
                guardian,
                child,
                GuardianChildLinkStatus.PENDING,
                guardian,
                NOW);
        link.accept(NOW);
        return link;
    }

    private UserAccount user(UserRole role, String email) {
        return new UserAccount(
                UUID.randomUUID(),
                email,
                "hash",
                email,
                role,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }
}
