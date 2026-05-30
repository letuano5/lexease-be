package com.lexease.scoring.provider;

import java.util.List;
import java.util.Map;

public record ScoringEvaluationResult(
        String requestId,
        String status,
        String heardText,
        String summary,
        Map<String, Object> scores,
        List<Map<String, Object>> words,
        List<String> difficultWords,
        String error
) {
}
