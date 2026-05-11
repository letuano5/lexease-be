package com.lexease.shared.api;

import java.util.List;

public record ApiError(String code, String message, List<?> details) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
