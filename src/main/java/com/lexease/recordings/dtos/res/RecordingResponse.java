package com.lexease.recordings.dtos.res;

import com.lexease.recordings.Recording;
import com.lexease.recordings.RecordingStatus;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record RecordingResponse(
        UUID id,
        UUID sessionId,
        UUID childId,
        UUID storyId,
        RecordingStatus status,
        Long durationMs,
        int wordCount,
        String expectedText,
        String mimeType,
        URI audioUrl,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        EvaluationResponse evaluation
) {
    public static RecordingResponse from(
            Recording recording,
            URI audioUrl,
            EvaluationResponse evaluation
    ) {
        return new RecordingResponse(
                recording.getId(),
                recording.getSession().getId(),
                recording.getChild().getId(),
                recording.getStory().getId(),
                recording.getStatus(),
                recording.getDurationMs(),
                recording.getWordCount(),
                recording.getExpectedText(),
                recording.getAudioMimeType(),
                audioUrl,
                recording.getExpiresAt(),
                recording.getCreatedAt(),
                recording.getUpdatedAt(),
                evaluation);
    }
}
