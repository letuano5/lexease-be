package com.lexease.progress.dtos.res;

import java.util.UUID;

public record ProgressSummaryResponse(
        UUID childId,
        String range,
        long totalPracticeMinutes,
        int sessionsCount,
        int completedSessionsCount,
        int recordedSessionsCount,
        double averageReadingSpeedWpm,
        double averageAccuracy,
        double averageFluency,
        double averagePace,
        double averageErrorsPerSession,
        long ttsHelpCount,
        ProgressTrendResponse trend
) {
}
