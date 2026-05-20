package com.lexease.reading.dtos.res;

import com.lexease.reading.ReadingSessionStatus;
import java.util.UUID;

public record ReadingSessionResponse(
        UUID sessionId,
        ReadingSessionStatus status,
        ReadingStoryResponse story,
        ReadingTtsResponse tts,
        ResumePositionResponse resumePosition,
        long elapsedMs
) {
}
