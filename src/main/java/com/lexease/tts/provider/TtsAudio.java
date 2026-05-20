package com.lexease.tts.provider;

public record TtsAudio(
        String mimeType,
        String format,
        Integer sampleRateHz,
        Integer durationMs,
        String contentBase64
) {
}
