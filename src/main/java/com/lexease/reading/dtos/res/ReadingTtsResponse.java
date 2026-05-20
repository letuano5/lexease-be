package com.lexease.reading.dtos.res;

import com.lexease.tts.TtsStatus;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public record ReadingTtsResponse(
        TtsStatus status,
        UUID assetId,
        String voice,
        URI audioUrl,
        List<WordTimingResponse> wordTimings
) {
}
