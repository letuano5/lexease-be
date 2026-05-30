package com.lexease.scoring.provider;

public interface ScoringProviderClient {
    ScoringJobSubmitResponse submitJob(ScoringEvaluationJobRequest request);

    ScoringEvaluationResult evaluateSync(ScoringEvaluationJobRequest request);
}
