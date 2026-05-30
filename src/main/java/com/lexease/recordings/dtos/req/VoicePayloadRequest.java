package com.lexease.recordings.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoicePayloadRequest(
        @NotBlank
        @Size(max = 100)
        String mimeType,

        @NotBlank
        String contentBase64
) {
}
