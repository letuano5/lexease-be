package com.lexease.tts.provider;

import java.util.Map;

public record TtsProviderError(
        String code,
        String message,
        Boolean retryable,
        Map<String, Object> details
) {
}
