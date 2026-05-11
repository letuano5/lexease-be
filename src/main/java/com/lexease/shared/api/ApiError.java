package com.lexease.shared.api;

import java.util.List;

public record ApiError(String code, String message, List<?> details) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message, List.of());
    }

    public static ApiError of(ErrorCode code, String message, List<?> details) {
        return new ApiError(code.name(), message, details);
    }
}
