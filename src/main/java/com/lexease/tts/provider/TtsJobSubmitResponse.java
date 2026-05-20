package com.lexease.tts.provider;

public record TtsJobSubmitResponse(
        String jobId,
        String requestId,
        String status
) {
}
