package com.lexease.users.dtos.req;

import com.lexease.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record PatchUserStatusRequest(@NotNull UserStatus status) {
}
