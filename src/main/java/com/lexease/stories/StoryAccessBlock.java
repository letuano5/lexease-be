package com.lexease.stories;

import com.lexease.users.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "story_access_blocks")
public class StoryAccessBlock {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_by_guardian_id", nullable = false)
    private UserAccount blockedByGuardian;

    private String reason;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoryAccessBlock() {
    }

    public StoryAccessBlock(
            UUID id,
            UserAccount child,
            Story story,
            UserAccount blockedByGuardian,
            String reason,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.child = child;
        this.story = story;
        this.blockedByGuardian = blockedByGuardian;
        this.reason = reason;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void updateReason(String reason, Instant updatedAt) {
        this.reason = reason;
        this.updatedAt = updatedAt;
    }

    public void deactivate(Instant updatedAt) {
        this.active = false;
        this.updatedAt = updatedAt;
    }
}
