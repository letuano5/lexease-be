package com.lexease.tts.provider;

public record TtsJobSubmitRequest(
        String requestId,
        String callbackUrl,
        String text,
        String voice,
        String audioFormat,
        String language
) {
}
