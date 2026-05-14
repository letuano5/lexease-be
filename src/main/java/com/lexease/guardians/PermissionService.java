package com.lexease.guardians;

import com.lexease.users.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final GuardianChildLinkRepository guardianChildLinkRepository;

    public PermissionService(GuardianChildLinkRepository guardianChildLinkRepository) {
        this.guardianChildLinkRepository = guardianChildLinkRepository;
    }

    public boolean canAccessChild(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (currentRole == UserRole.ADMIN) {
            return true;
        }
        if (currentRole == UserRole.CHILD) {
            return currentUserId.equals(childId);
        }
        return currentRole == UserRole.GUARDIAN
                && guardianChildLinkRepository.existsByGuardianIdAndChildIdAndStatus(
                currentUserId,
                childId,
                GuardianChildLinkStatus.ACCEPTED);
    }

    public boolean canManageChild(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (currentRole == UserRole.ADMIN) {
            return true;
        }
        return currentRole == UserRole.GUARDIAN
                && guardianChildLinkRepository.existsByGuardianIdAndChildIdAndStatus(
                currentUserId,
                childId,
                GuardianChildLinkStatus.ACCEPTED);
    }

    public boolean canManageStory(UserRole currentRole) {
        return currentRole == UserRole.ADMIN;
    }

    public boolean canManageDisplaySettings(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (currentRole == UserRole.CHILD) {
            return currentUserId.equals(childId);
        }
        return canManageChild(currentUserId, currentRole, childId);
    }
}
