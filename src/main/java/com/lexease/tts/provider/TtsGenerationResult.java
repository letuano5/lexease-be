package com.lexease.tts.provider;

import java.util.List;
import java.util.Map;

public record TtsGenerationResult(
        String requestId,
        String voice,
        String language,
        TtsAudio audio,
        List<TtsProviderWord> words,
        Map<String, Object> metadata
) {
}
