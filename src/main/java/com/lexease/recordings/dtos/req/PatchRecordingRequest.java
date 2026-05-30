package com.lexease.recordings.dtos.req;

import jakarta.validation.constraints.PositiveOrZero;

public record PatchRecordingRequest(
        @PositiveOrZero
        Long durationMs
) {
}
