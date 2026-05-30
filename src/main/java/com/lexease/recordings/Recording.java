package com.lexease.recordings;

import com.lexease.reading.ReadingSession;
import com.lexease.stories.Story;
import com.lexease.users.UserAccount;
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
@Table(name = "recordings")
public class Recording {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ReadingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordingStatus status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Column(name = "expected_text", nullable = false)
    private String expectedText;

    @Column(name = "audio_object_key", nullable = false)
    private String audioObjectKey;

    @Column(name = "audio_mime_type", nullable = false)
    private String audioMimeType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recording() {
    }

    public Recording(
            UUID id,
            ReadingSession session,
            Long durationMs,
            int wordCount,
            String expectedText,
            String audioObjectKey,
            String audioMimeType,
            Instant expiresAt,
            Instant now
    ) {
        this.id = id;
        this.session = session;
        this.child = session.getChild();
        this.story = session.getStory();
        this.status = RecordingStatus.READY;
        this.durationMs = durationMs;
        this.wordCount = wordCount;
        this.expectedText = expectedText;
        this.audioObjectKey = audioObjectKey;
        this.audioMimeType = audioMimeType;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public ReadingSession getSession() {
        return session;
    }

    public UserAccount getChild() {
        return child;
    }

    public Story getStory() {
        return story;
    }

    public RecordingStatus getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public int getWordCount() {
        return wordCount;
    }

    public String getExpectedText() {
        return expectedText;
    }

    public String getAudioObjectKey() {
        return audioObjectKey;
    }

    public String getAudioMimeType() {
        return audioMimeType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
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

    public void update(Long durationMs, Instant now) {
        this.durationMs = durationMs;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.status = RecordingStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
