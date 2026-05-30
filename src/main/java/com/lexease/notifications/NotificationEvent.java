package com.lexease.notifications;

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
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_events")
public class NotificationEvent {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ReminderSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "deep_link", nullable = false)
    private String deepLink;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "practice_started_at")
    private Instant practiceStartedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationEvent() {
    }

    public NotificationEvent(UUID id, ReminderSchedule schedule, UserAccount child, String deepLink, Instant scheduledFor, Instant now) {
        this.id = id;
        this.schedule = schedule;
        this.child = child;
        this.status = NotificationStatus.SCHEDULED;
        this.deepLink = deepLink;
        this.scheduledFor = scheduledFor;
        this.metadata = Map.of();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public ReminderSchedule getSchedule() {
        return schedule;
    }

    public UserAccount getChild() {
        return child;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getPracticeStartedAt() {
        return practiceStartedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.failureReason = null;
        this.updatedAt = now;
    }

    public void markFailed(String failureReason, Instant now) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = failureReason;
        this.updatedAt = now;
    }

    public void markOpened(NotificationStatus openedStatus, Instant occurredAt, Instant now) {
        this.status = openedStatus;
        this.openedAt = occurredAt;
        this.updatedAt = now;
    }

    public void markPracticeStarted(Instant occurredAt, Instant now) {
        this.status = NotificationStatus.PRACTICE_STARTED;
        this.practiceStartedAt = occurredAt;
        if (this.openedAt == null) {
            this.openedAt = occurredAt;
        }
        this.updatedAt = now;
    }

    public void markIgnored(Instant now) {
        this.status = NotificationStatus.IGNORED;
        this.updatedAt = now;
    }
}
