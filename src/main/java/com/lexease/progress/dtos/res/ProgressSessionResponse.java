package com.lexease.progress.dtos.res;

import com.lexease.reading.ReadingSessionStatus;
import com.lexease.recordings.EvaluationStatus;
import java.time.Instant;
import java.util.UUID;

public record ProgressSessionResponse(
        UUID sessionId,
        UUID storyId,
        String storyTitle,
        ReadingSessionStatus status,
        Instant startedAt,
        Instant completedAt,
        long elapsedMs,
        int currentWordIndex,
        double readingSpeedWpm,
        int recordingCount,
        EvaluationStatus latestEvaluationStatus,
        Double latestAccuracy
) {
}
