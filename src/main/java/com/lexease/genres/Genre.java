package com.lexease.genres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "genres")
public class Genre {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Genre() {
    }

    public Genre(UUID id, String name, String normalizedName, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(String name, String normalizedName, Instant updatedAt) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.updatedAt = updatedAt;
    }

    public void delete(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }
}
