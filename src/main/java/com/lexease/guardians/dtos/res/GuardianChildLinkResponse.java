package com.lexease.guardians.dtos.res;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.users.dtos.res.UserResponse;
import java.util.UUID;

public record GuardianChildLinkResponse(
        UUID linkId,
        UUID guardianId,
        UUID childId,
        UserResponse child,
        GuardianChildLinkStatus status
) {
    public static GuardianChildLinkResponse from(GuardianChildLink link) {
        return new GuardianChildLinkResponse(
                link.getId(),
                link.getGuardian().getId(),
                link.getChild().getId(),
                UserResponse.from(link.getChild()),
                link.getStatus());
    }
}
