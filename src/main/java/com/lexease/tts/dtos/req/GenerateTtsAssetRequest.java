package com.lexease.tts.dtos.req;

import jakarta.validation.constraints.Size;

public record GenerateTtsAssetRequest(
        @Size(max = 100) String voice,
        Boolean refresh
) {
    public boolean refreshRequested() {
        return Boolean.TRUE.equals(refresh);
    }
}
