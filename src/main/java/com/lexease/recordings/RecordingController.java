package com.lexease.recordings;

import com.lexease.recordings.dtos.req.CreateRecordingRequest;
import com.lexease.recordings.dtos.req.PatchRecordingRequest;
import com.lexease.recordings.dtos.res.EvaluationResponse;
import com.lexease.recordings.dtos.res.RecordingResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecordingController {
    private final RecordingService recordingService;

    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @PostMapping("/sessions/{sessionId}/recordings")
    @ResponseStatus(HttpStatus.CREATED)
    RecordingResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateRecordingRequest request
    ) {
        return recordingService.create(principal.id(), principal.role(), sessionId, request);
    }

    @GetMapping("/recordings/{id}")
    RecordingResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return recordingService.get(principal.id(), principal.role(), id);
    }

    @PatchMapping("/recordings/{id}")
    RecordingResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PatchRecordingRequest request
    ) {
        return recordingService.update(principal.id(), principal.role(), id, request);
    }

    @DeleteMapping("/recordings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        recordingService.delete(principal.id(), principal.role(), id);
    }

    @GetMapping("/recordings/{id}/evaluation")
    EvaluationResponse getRecordingEvaluation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return recordingService.getEvaluation(principal.id(), principal.role(), id);
    }

    @GetMapping("/evaluations/{id}")
    EvaluationResponse getEvaluation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return recordingService.getEvaluationById(principal.id(), principal.role(), id);
    }

    @PostMapping("/evaluations/{id}/retry")
    EvaluationResponse retry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return recordingService.retry(principal.id(), principal.role(), id);
    }

    @DeleteMapping("/evaluations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteEvaluation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        recordingService.deleteEvaluation(principal.id(), principal.role(), id);
    }
}
