package com.lexease.recordings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_evaluations")
public class AiEvaluation {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recording_id", nullable = false)
    private Recording recording;

    @Column(nullable = false)
    private String provider;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "provider_job_id")
    private String providerJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @Column(name = "heard_text")
    private String heardText;

    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> scores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_results", nullable = false)
    private List<Map<String, Object>> wordResults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficult_words", nullable = false)
    private List<String> difficultWords;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiEvaluation() {
    }

    public AiEvaluation(UUID id, Recording recording, String provider, String modelName, String promptVersion, Instant now) {
        this.id = id;
        this.recording = recording;
        this.provider = provider;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = EvaluationStatus.PENDING;
        this.scores = Map.of();
        this.wordResults = List.of();
        this.difficultWords = List.of();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Recording getRecording() {
        return recording;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getProviderJobId() {
        return providerJobId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public String getHeardText() {
        return heardText;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getScores() {
        return scores;
    }

    public List<Map<String, Object>> getWordResults() {
        return wordResults;
    }

    public List<String> getDifficultWords() {
        return difficultWords;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing(String providerJobId, Instant now) {
        this.status = EvaluationStatus.PROCESSING;
        this.providerJobId = providerJobId;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void markDone(
            String heardText,
            String summary,
            Map<String, Object> scores,
            List<Map<String, Object>> wordResults,
            List<String> difficultWords,
            Instant now
    ) {
        this.status = EvaluationStatus.DONE;
        this.heardText = heardText;
        this.summary = summary;
        this.scores = scores == null ? Map.of() : scores;
        this.wordResults = wordResults == null ? List.of() : wordResults;
        this.difficultWords = difficultWords == null ? List.of() : difficultWords;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, Instant now) {
        this.status = EvaluationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
