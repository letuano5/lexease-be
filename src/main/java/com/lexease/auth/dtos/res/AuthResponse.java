package com.lexease.auth.dtos.res;

import com.lexease.users.dtos.res.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user
) {
}
