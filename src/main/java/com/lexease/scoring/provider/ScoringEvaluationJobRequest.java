package com.lexease.scoring.provider;

import java.util.UUID;

public record ScoringEvaluationJobRequest(
        String requestId,
        String callbackUrl,
        UUID childId,
        UUID sessionId,
        UUID recordingId,
        ScoringStoryPayload story,
        ScoringAudioPayload audio,
        String language,
        String provider,
        String model,
        String promptVersion
) {
}
