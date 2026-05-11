package com.lexease.guardians;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lexease.users.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTests {
    @Mock
    private GuardianChildLinkRepository linkRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void childCanAccessOnlySelf() {
        UUID childId = UUID.randomUUID();

        assertThat(permissionService.canAccessChild(childId, UserRole.CHILD, childId)).isTrue();
        assertThat(permissionService.canAccessChild(UUID.randomUUID(), UserRole.CHILD, childId)).isFalse();
    }

    @Test
    void guardianCanManageAcceptedChildOnly() {
        UUID guardianId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        when(linkRepository.existsByGuardianIdAndChildIdAndStatus(
                guardianId,
                childId,
                GuardianChildLinkStatus.ACCEPTED)).thenReturn(true);

        assertThat(permissionService.canManageChild(guardianId, UserRole.GUARDIAN, childId)).isTrue();
    }

    @Test
    void adminCanManageStories() {
        assertThat(permissionService.canManageStory(UserRole.ADMIN)).isTrue();
        assertThat(permissionService.canManageStory(UserRole.GUARDIAN)).isFalse();
        assertThat(permissionService.canManageStory(UserRole.CHILD)).isFalse();
    }
}
