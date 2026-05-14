package com.lexease.stories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authors")
public class Author {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Author() {
    }

    public Author(UUID id, String name, String normalizedName, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.createdAt = createdAt;
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
}
