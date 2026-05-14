package com.lexease.users.dtos.res;

import com.lexease.users.UserAccount;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        UserStatus status
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus());
    }
}
