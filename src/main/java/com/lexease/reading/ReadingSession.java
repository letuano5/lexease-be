package com.lexease.reading;

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
@Table(name = "reading_sessions")
public class ReadingSession {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "story_version", nullable = false)
    private int storyVersion;

    @Column(name = "voice_id", nullable = false)
    private String voiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingSessionStatus status;

    @Column(name = "current_word_index", nullable = false)
    private int currentWordIndex;

    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ReadingSession() {
    }

    public ReadingSession(UUID id, UserAccount child, Story story, String voiceId, Instant now) {
        this.id = id;
        this.child = child;
        this.story = story;
        this.storyVersion = story.getVersion();
        this.voiceId = voiceId;
        this.status = ReadingSessionStatus.IN_PROGRESS;
        this.currentWordIndex = 0;
        this.elapsedMs = 0;
        this.startedAt = now;
        this.lastActiveAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getChild() {
        return child;
    }

    public Story getStory() {
        return story;
    }

    public int getStoryVersion() {
        return storyVersion;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public ReadingSessionStatus getStatus() {
        return status;
    }

    public int getCurrentWordIndex() {
        return currentWordIndex;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void touch(Instant now) {
        this.lastActiveAt = now;
    }

    public void updateProgress(int currentWordIndex, long elapsedMs, Instant now) {
        this.currentWordIndex = Math.max(this.currentWordIndex, currentWordIndex);
        this.elapsedMs = Math.max(this.elapsedMs, elapsedMs);
        this.lastActiveAt = now;
    }

    public void complete(Instant now) {
        this.status = ReadingSessionStatus.COMPLETED;
        this.completedAt = now;
        this.lastActiveAt = now;
    }
}
