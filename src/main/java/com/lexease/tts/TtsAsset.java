package com.lexease.tts;

import com.lexease.stories.Story;
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
import java.util.UUID;

@Entity
@Table(name = "tts_assets")
public class TtsAsset {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "story_version", nullable = false)
    private int storyVersion;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_request_id")
    private String providerRequestId;

    @Column(name = "provider_job_id")
    private String providerJobId;

    @Column(name = "voice_id", nullable = false)
    private String voiceId;

    @Column(name = "audio_object_key")
    private String audioObjectKey;

    @Column(name = "audio_mime_type")
    private String audioMimeType;

    @Column(name = "audio_duration_ms")
    private Integer audioDurationMs;

    @Column(name = "audio_sample_rate_hz")
    private Integer audioSampleRateHz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TtsStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TtsAsset() {
    }

    public TtsAsset(UUID id, Story story, int storyVersion, String provider, String voiceId, Instant now) {
        this.id = id;
        this.story = story;
        this.storyVersion = storyVersion;
        this.provider = provider;
        this.voiceId = voiceId;
        this.status = TtsStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Story getStory() {
        return story;
    }

    public int getStoryVersion() {
        return storyVersion;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public String getProviderJobId() {
        return providerJobId;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public String getAudioObjectKey() {
        return audioObjectKey;
    }

    public String getAudioMimeType() {
        return audioMimeType;
    }

    public Integer getAudioDurationMs() {
        return audioDurationMs;
    }

    public Integer getAudioSampleRateHz() {
        return audioSampleRateHz;
    }

    public TtsStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void startProcessing(String requestId, Instant now) {
        this.providerRequestId = requestId;
        this.providerJobId = null;
        this.audioObjectKey = null;
        this.audioMimeType = null;
        this.audioDurationMs = null;
        this.audioSampleRateHz = null;
        this.errorMessage = null;
        this.status = TtsStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markSubmitted(String providerJobId, Instant now) {
        this.providerJobId = providerJobId;
        this.status = TtsStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markReady(
            String audioObjectKey,
            String audioMimeType,
            Integer audioDurationMs,
            Integer audioSampleRateHz,
            Instant now
    ) {
        this.audioObjectKey = audioObjectKey;
        this.audioMimeType = audioMimeType;
        this.audioDurationMs = audioDurationMs;
        this.audioSampleRateHz = audioSampleRateHz;
        this.errorMessage = null;
        this.status = TtsStatus.READY;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, Instant now) {
        this.errorMessage = errorMessage;
        this.status = TtsStatus.FAILED;
        this.updatedAt = now;
    }
}
