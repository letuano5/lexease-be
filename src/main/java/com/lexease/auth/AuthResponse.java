package com.lexease.auth;

import com.lexease.users.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user
) {
}
