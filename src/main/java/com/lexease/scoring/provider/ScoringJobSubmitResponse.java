package com.lexease.scoring.provider;

public record ScoringJobSubmitResponse(
        String jobId,
        String status,
        String error
) {
}
