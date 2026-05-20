package com.lexease.tts.provider;

public record TtsProviderWord(
        int index,
        String text,
        String normalizedText,
        int startMs,
        int endMs
) {
}
