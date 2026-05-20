package com.lexease.tts.dtos.res;

public record TtsAssetGenerationResult(
        TtsAssetResponse response,
        boolean accepted
) {
}
