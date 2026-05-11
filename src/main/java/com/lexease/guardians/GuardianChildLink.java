package com.lexease.guardians;

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
@Table(name = "guardian_child_links")
public class GuardianChildLink {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private UserAccount guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private UserAccount child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuardianChildLinkStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private UserAccount invitedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected GuardianChildLink() {
    }

    public GuardianChildLink(
            UUID id,
            UserAccount guardian,
            UserAccount child,
            GuardianChildLinkStatus status,
            UserAccount invitedBy,
            Instant createdAt
    ) {
        this.id = id;
        this.guardian = guardian;
        this.child = child;
        this.status = status;
        this.invitedBy = invitedBy;
        this.createdAt = createdAt;
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

    public GuardianChildLinkStatus getStatus() {
        return status;
    }

    public UserAccount getInvitedBy() {
        return invitedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void accept(Instant acceptedAt) {
        this.status = GuardianChildLinkStatus.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    public void reject() {
        this.status = GuardianChildLinkStatus.REJECTED;
    }

    public void revoke() {
        this.status = GuardianChildLinkStatus.REVOKED;
    }
}
