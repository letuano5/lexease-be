package com.lexease.progress.dtos.res;

public record ProgressTrendResponse(
        String practiceMinutes,
        String readingSpeed,
        String accuracy,
        String errors
) {
}
