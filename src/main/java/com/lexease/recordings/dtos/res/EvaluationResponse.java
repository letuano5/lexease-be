package com.lexease.recordings.dtos.res;

import com.lexease.recordings.AiEvaluation;
import com.lexease.recordings.EvaluationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID recordingId,
        EvaluationStatus status,
        String provider,
        String modelName,
        String promptVersion,
        String providerJobId,
        String heardText,
        String summary,
        Map<String, Object> scores,
        List<Map<String, Object>> words,
        List<String> difficultWords,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static EvaluationResponse from(AiEvaluation evaluation) {
        if (evaluation == null) {
            return null;
        }
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getRecording().getId(),
                evaluation.getStatus(),
                evaluation.getProvider(),
                evaluation.getModelName(),
                evaluation.getPromptVersion(),
                evaluation.getProviderJobId(),
                evaluation.getHeardText(),
                evaluation.getSummary(),
                evaluation.getScores(),
                evaluation.getWordResults(),
                evaluation.getDifficultWords(),
                evaluation.getErrorMessage(),
                evaluation.getCreatedAt(),
                evaluation.getUpdatedAt());
    }
}
