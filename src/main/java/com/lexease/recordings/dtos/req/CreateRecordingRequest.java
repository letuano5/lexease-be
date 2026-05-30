package com.lexease.recordings.dtos.req;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRecordingRequest(
        @PositiveOrZero
        Long durationMs,

        @NotBlank
        @Size(max = 20000)
        String expectedText,

        @NotNull
        @Valid
        VoicePayloadRequest voice
) {
}
