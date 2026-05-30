package com.lexease.progress.dtos.res;

import java.time.LocalDate;

public record ProgressBucketResponse(
        LocalDate date,
        long practiceMinutes,
        int sessionsCount,
        int recordedSessionsCount,
        double averageReadingSpeedWpm,
        double averageAccuracy,
        double averageErrorsPerSession,
        long ttsHelpCount
) {
}
