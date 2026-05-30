package com.lexease.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexease.recordings.RecordingService;
import com.lexease.scoring.provider.ScoringEvaluationResult;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/scoring/evaluations")
public class InternalScoringCallbackController {
    private final ScoringCallbackVerifier callbackVerifier;
    private final RecordingService recordingService;
    private final ObjectMapper objectMapper;

    public InternalScoringCallbackController(
            ScoringCallbackVerifier callbackVerifier,
            RecordingService recordingService,
            ObjectMapper objectMapper
    ) {
        this.callbackVerifier = callbackVerifier;
        this.recordingService = recordingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{evaluationId}/callback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void callback(
            @PathVariable UUID evaluationId,
            @RequestHeader("X-Lexease-Timestamp") String timestamp,
            @RequestHeader("X-Lexease-Signature") String signature,
            @RequestBody String rawBody
    ) {
        callbackVerifier.verify(timestamp, signature, rawBody);
        try {
            ScoringEvaluationResult request = objectMapper.readValue(rawBody, ScoringEvaluationResult.class);
            recordingService.handleCallback(evaluationId, request);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SCORING_CALLBACK, "Invalid callback body");
        }
    }
}
