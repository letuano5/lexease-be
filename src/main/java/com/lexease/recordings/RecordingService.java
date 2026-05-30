package com.lexease.recordings;

import com.lexease.guardians.PermissionService;
import com.lexease.reading.ReadingSession;
import com.lexease.reading.ReadingSessionRepository;
import com.lexease.recordings.dtos.req.CreateRecordingRequest;
import com.lexease.recordings.dtos.req.PatchRecordingRequest;
import com.lexease.recordings.dtos.res.EvaluationResponse;
import com.lexease.recordings.dtos.res.RecordingResponse;
import com.lexease.scoring.ScoringProperties;
import com.lexease.scoring.ScoringProviderMode;
import com.lexease.scoring.provider.ScoringAudioPayload;
import com.lexease.scoring.provider.ScoringEvaluationJobRequest;
import com.lexease.scoring.provider.ScoringEvaluationResult;
import com.lexease.scoring.provider.ScoringJobSubmitResponse;
import com.lexease.scoring.provider.ScoringProviderClient;
import com.lexease.scoring.provider.ScoringStoryPayload;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.text.TextNormalizer;
import com.lexease.storage.ObjectStorageService;
import com.lexease.users.UserRole;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordingService {
    private static final String LANGUAGE = "vi-VN";

    private final RecordingRepository recordingRepository;
    private final AiEvaluationRepository evaluationRepository;
    private final ReadingSessionRepository sessionRepository;
    private final PermissionService permissionService;
    private final ObjectStorageService objectStorageService;
    private final ScoringProviderClient scoringProviderClient;
    private final ScoringProperties scoringProperties;
    private final Clock clock;

    public RecordingService(
            RecordingRepository recordingRepository,
            AiEvaluationRepository evaluationRepository,
            ReadingSessionRepository sessionRepository,
            PermissionService permissionService,
            ObjectStorageService objectStorageService,
            ScoringProviderClient scoringProviderClient,
            ScoringProperties scoringProperties,
            Clock clock
    ) {
        this.recordingRepository = recordingRepository;
        this.evaluationRepository = evaluationRepository;
        this.sessionRepository = sessionRepository;
        this.permissionService = permissionService;
        this.objectStorageService = objectStorageService;
        this.scoringProviderClient = scoringProviderClient;
        this.scoringProperties = scoringProperties;
        this.clock = clock;
    }

    @Transactional
    public RecordingResponse create(UUID currentUserId, UserRole role, UUID sessionId, CreateRecordingRequest request) {
        if (role != UserRole.CHILD) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.RECORDING_FORBIDDEN, "Only children can create recordings");
        }
        ReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.READING_SESSION_NOT_FOUND, "Reading session not found"));
        if (!session.getChild().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.RECORDING_FORBIDDEN, "Cannot create recording for this session");
        }
        String expectedText = validateExpectedText(session.getStory().getContent(), request.expectedText());

        Instant now = Instant.now(clock);
        UUID recordingId = UUID.randomUUID();
        String objectKey = saveAudio(currentUserId, recordingId, request);
        Recording recording = recordingRepository.save(new Recording(
                recordingId,
                session,
                request.durationMs(),
                session.getStory().getWords().size(),
                expectedText,
                objectKey,
                request.voice().mimeType(),
                now.plus(scoringProperties.recordingRetention()),
                now));
        AiEvaluation evaluation = evaluationRepository.save(new AiEvaluation(
                UUID.randomUUID(),
                recording,
                scoringProperties.provider(),
                scoringProperties.model(),
                scoringProperties.promptVersion(),
                now));
        submitEvaluation(evaluation, recording, now);
        return toResponse(recording, evaluation);
    }

    @Transactional(readOnly = true)
    public RecordingResponse get(UUID currentUserId, UserRole role, UUID recordingId) {
        Recording recording = requireVisibleRecording(currentUserId, role, recordingId);
        return toResponse(recording);
    }

    @Transactional
    public RecordingResponse update(UUID currentUserId, UserRole role, UUID recordingId, PatchRecordingRequest request) {
        Recording recording = requireManageableRecording(currentUserId, role, recordingId);
        recording.update(request.durationMs(), Instant.now(clock));
        return toResponse(recording);
    }

    @Transactional
    public void delete(UUID currentUserId, UserRole role, UUID recordingId) {
        Recording recording = requireManageableRecording(currentUserId, role, recordingId);
        Instant now = Instant.now(clock);
        recording.markDeleted(now);
        evaluationRepository.findFirstByRecordingIdAndDeletedAtIsNullOrderByCreatedAtDesc(recording.getId())
                .ifPresent(evaluation -> evaluation.markDeleted(now));
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluation(UUID currentUserId, UserRole role, UUID recordingId) {
        Recording recording = requireVisibleRecording(currentUserId, role, recordingId);
        AiEvaluation evaluation = evaluationRepository.findFirstByRecordingIdAndDeletedAtIsNullOrderByCreatedAtDesc(recording.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.EVALUATION_NOT_FOUND, "Evaluation not found"));
        return EvaluationResponse.from(evaluation);
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluationById(UUID currentUserId, UserRole role, UUID evaluationId) {
        AiEvaluation evaluation = requireVisibleEvaluation(currentUserId, role, evaluationId);
        return EvaluationResponse.from(evaluation);
    }

    @Transactional
    public EvaluationResponse retry(UUID currentUserId, UserRole role, UUID evaluationId) {
        AiEvaluation evaluation = requireManageableEvaluation(currentUserId, role, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_EVALUATION_STATE, "Only failed evaluations can be retried");
        }
        Recording recording = evaluation.getRecording();
        submitEvaluation(evaluation, recording, Instant.now(clock));
        return EvaluationResponse.from(evaluation);
    }

    @Transactional
    public void deleteEvaluation(UUID currentUserId, UserRole role, UUID evaluationId) {
        AiEvaluation evaluation = requireManageableEvaluation(currentUserId, role, evaluationId);
        evaluation.markDeleted(Instant.now(clock));
    }

    @Transactional
    public void handleCallback(UUID evaluationId, ScoringEvaluationResult result) {
        AiEvaluation evaluation = evaluationRepository.findByIdAndDeletedAtIsNull(evaluationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.EVALUATION_NOT_FOUND, "Evaluation not found"));
        if (result.requestId() != null && !result.requestId().equals(evaluationId.toString())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SCORING_CALLBACK, "Callback request id mismatch");
        }
        applyProviderResult(evaluation, result, Instant.now(clock));
    }

    private String saveAudio(UUID childId, UUID recordingId, CreateRecordingRequest request) {
        byte[] audio = decodeBase64(request.voice().contentBase64());
        String objectKey = "recordings/%s/%s/audio.bin".formatted(childId, recordingId);
        objectStorageService.putObject(objectKey, audio, request.voice().mimeType());
        return objectKey;
    }

    private void submitEvaluation(AiEvaluation evaluation, Recording recording, Instant now) {
        try {
            ScoringEvaluationJobRequest request = toProviderRequest(evaluation, recording);
            if (scoringProperties.mode() == ScoringProviderMode.SYNC) {
                applyProviderResult(evaluation, scoringProviderClient.evaluateSync(request), now);
            } else {
                ScoringJobSubmitResponse response = scoringProviderClient.submitJob(request);
                if (response != null && "FAILED".equalsIgnoreCase(response.status())) {
                    evaluation.markFailed(response.error() == null ? "Scoring provider failed" : response.error(), now);
                } else {
                    evaluation.markProcessing(response == null ? null : response.jobId(), now);
                }
            }
        } catch (RuntimeException ex) {
            evaluation.markFailed(ex.getMessage(), now);
        }
    }

    private ScoringEvaluationJobRequest toProviderRequest(AiEvaluation evaluation, Recording recording) {
        return new ScoringEvaluationJobRequest(
                evaluation.getId().toString(),
                scoringProperties.callbackBaseUrl() + "/internal/scoring/evaluations/" + evaluation.getId() + "/callback",
                recording.getChild().getId(),
                recording.getSession().getId(),
                recording.getId(),
                new ScoringStoryPayload(recording.getStory().getId(), recording.getStory().getTitle(), recording.getExpectedText()),
                new ScoringAudioPayload(
                        objectStorageService.getSignedReadUrl(recording.getAudioObjectKey()),
                        recording.getAudioMimeType(),
                        recording.getDurationMs()),
                LANGUAGE,
                evaluation.getProvider(),
                evaluation.getModelName(),
                evaluation.getPromptVersion());
    }

    private void applyProviderResult(AiEvaluation evaluation, ScoringEvaluationResult result, Instant now) {
        if (result == null) {
            evaluation.markFailed("Empty scoring provider response", now);
            return;
        }
        if ("FAILED".equalsIgnoreCase(result.status())) {
            evaluation.markFailed(result.error() == null ? "Scoring provider failed" : result.error(), now);
            return;
        }
        if (!"DONE".equalsIgnoreCase(result.status())) {
            evaluation.markFailed("Unsupported scoring status: " + result.status(), now);
            return;
        }
        evaluation.markDone(
                result.heardText(),
                result.summary(),
                result.scores(),
                result.words(),
                result.difficultWords(),
                now);
    }

    private String validateExpectedText(String storyText, String expectedText) {
        String trimmed = expectedText.trim();
        String normalizedStory = TextNormalizer.normalizeForSearch(storyText);
        String normalizedExpected = TextNormalizer.normalizeForSearch(trimmed);
        if (normalizedExpected.isBlank() || !normalizedStory.contains(normalizedExpected)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Expected text must belong to the session story");
        }
        return trimmed;
    }

    private Recording requireVisibleRecording(UUID currentUserId, UserRole role, UUID recordingId) {
        Recording recording = recordingRepository.findByIdAndDeletedAtIsNull(recordingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RECORDING_NOT_FOUND, "Recording not found"));
        if (!permissionService.canAccessChild(currentUserId, role, recording.getChild().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.RECORDING_FORBIDDEN, "Cannot access recording");
        }
        return recording;
    }

    private Recording requireManageableRecording(UUID currentUserId, UserRole role, UUID recordingId) {
        Recording recording = recordingRepository.findByIdAndDeletedAtIsNull(recordingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RECORDING_NOT_FOUND, "Recording not found"));
        boolean canManage = role == UserRole.CHILD && currentUserId.equals(recording.getChild().getId())
                || permissionService.canManageChild(currentUserId, role, recording.getChild().getId());
        if (!canManage) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.RECORDING_FORBIDDEN, "Cannot manage recording");
        }
        return recording;
    }

    private AiEvaluation requireVisibleEvaluation(UUID currentUserId, UserRole role, UUID evaluationId) {
        AiEvaluation evaluation = evaluationRepository.findByIdAndDeletedAtIsNull(evaluationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.EVALUATION_NOT_FOUND, "Evaluation not found"));
        if (!permissionService.canAccessChild(currentUserId, role, evaluation.getRecording().getChild().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.EVALUATION_FORBIDDEN, "Cannot access evaluation");
        }
        return evaluation;
    }

    private AiEvaluation requireManageableEvaluation(UUID currentUserId, UserRole role, UUID evaluationId) {
        AiEvaluation evaluation = evaluationRepository.findByIdAndDeletedAtIsNull(evaluationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.EVALUATION_NOT_FOUND, "Evaluation not found"));
        if (!permissionService.canManageChild(currentUserId, role, evaluation.getRecording().getChild().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.EVALUATION_FORBIDDEN, "Cannot manage evaluation");
        }
        return evaluation;
    }

    private RecordingResponse toResponse(Recording recording) {
        AiEvaluation evaluation = evaluationRepository.findFirstByRecordingIdAndDeletedAtIsNullOrderByCreatedAtDesc(recording.getId())
                .orElse(null);
        return toResponse(recording, evaluation);
    }

    private RecordingResponse toResponse(Recording recording, AiEvaluation evaluation) {
        return RecordingResponse.from(recording, signedUrl(recording), EvaluationResponse.from(evaluation));
    }

    private URI signedUrl(Recording recording) {
        return objectStorageService.getSignedReadUrl(recording.getAudioObjectKey());
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Invalid audio base64");
        }
    }

}
