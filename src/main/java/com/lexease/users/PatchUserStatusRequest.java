package com.lexease.users;

import jakarta.validation.constraints.NotNull;

public record PatchUserStatusRequest(@NotNull UserStatus status) {
}
