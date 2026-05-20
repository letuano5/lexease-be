package com.lexease.tts.provider;

public record TtsJobStatusResponse(
        String jobId,
        String requestId,
        String status,
        String callbackStatus,
        Integer callbackAttempts,
        TtsProviderError error,
        TtsGenerationResult result
) {
}
