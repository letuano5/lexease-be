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
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevicePlatform platform;

    @Column(name = "device_id")
    private String deviceId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeviceToken() {
    }

    public DeviceToken(UUID id, UserAccount user, DevicePlatform platform, String deviceId, String token, Instant now) {
        this.id = id;
        this.user = user;
        this.platform = platform;
        this.deviceId = deviceId;
        this.token = token;
        this.active = true;
        this.lastSeenAt = now;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getToken() {
        return token;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void refresh(UserAccount user, DevicePlatform platform, String deviceId, Instant now) {
        this.user = user;
        this.platform = platform;
        this.deviceId = deviceId;
        this.active = true;
        this.lastSeenAt = now;
    }

    public void deactivate() {
        this.active = false;
    }
}
