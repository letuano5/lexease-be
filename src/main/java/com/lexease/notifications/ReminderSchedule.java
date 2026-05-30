package com.lexease.notifications;

import com.lexease.users.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reminder_schedules")
public class ReminderSchedule {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private UserAccount guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "days_of_week", nullable = false)
    private String[] daysOfWeek;

    @Column(name = "local_time", nullable = false)
    private LocalTime localTime;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReminderSchedule() {
    }

    public ReminderSchedule(
            UUID id,
            UserAccount guardian,
            UserAccount child,
            List<String> daysOfWeek,
            LocalTime localTime,
            String timezone,
            String message,
            Instant nextRunAt,
            Instant now
    ) {
        this.id = id;
        this.guardian = guardian;
        this.child = child;
        this.daysOfWeek = daysOfWeek.toArray(String[]::new);
        this.localTime = localTime;
        this.timezone = timezone;
        this.message = message;
        this.enabled = true;
        this.nextRunAt = nextRunAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getGuardian() {
        return guardian;
    }

    public UserAccount getChild() {
        return child;
    }

    public List<String> getDaysOfWeek() {
        return Arrays.asList(daysOfWeek);
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(List<String> daysOfWeek, LocalTime localTime, String timezone, String message, boolean enabled, Instant nextRunAt, Instant now) {
        this.daysOfWeek = daysOfWeek.toArray(String[]::new);
        this.localTime = localTime;
        this.timezone = timezone;
        this.message = message;
        this.enabled = enabled;
        this.nextRunAt = nextRunAt;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        this.enabled = false;
        this.nextRunAt = null;
        this.updatedAt = now;
    }

    public void advance(Instant nextRunAt, Instant now) {
        this.nextRunAt = nextRunAt;
        this.updatedAt = now;
    }
}
