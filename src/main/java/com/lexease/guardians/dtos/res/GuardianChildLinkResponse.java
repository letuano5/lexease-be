package com.lexease.guardians.dtos.res;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkStatus;
import java.util.UUID;

public record GuardianChildLinkResponse(
        UUID linkId,
        UUID guardianId,
        UUID childId,
        GuardianChildLinkStatus status
) {
    public static GuardianChildLinkResponse from(GuardianChildLink link) {
        return new GuardianChildLinkResponse(
                link.getId(),
                link.getGuardian().getId(),
                link.getChild().getId(),
                link.getStatus());
    }
}
