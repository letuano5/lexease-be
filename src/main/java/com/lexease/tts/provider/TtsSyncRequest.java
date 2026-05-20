package com.lexease.tts.provider;

public record TtsSyncRequest(
        String text,
        String voice,
        String audioFormat,
        String language
) {
}
