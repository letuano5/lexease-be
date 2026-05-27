package com.lexease.tts.dtos.req;

import com.lexease.tts.provider.TtsAudio;
import com.lexease.tts.provider.TtsProviderError;
import com.lexease.tts.provider.TtsProviderWord;
import java.util.List;
import java.util.Map;

public record TtsCallbackRequest(
        String jobId,
        String status,
        String requestId,
        String voice,
        String language,
        TtsAudio audio,
        List<TtsProviderWord> words,
        TtsProviderError error,
        Map<String, Object> metadata
) {
}
