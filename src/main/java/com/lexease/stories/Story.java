package com.lexease.stories;

import com.lexease.users.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stories")
public class Story {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoryStatus status;

    @Column(nullable = false)
    private int version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
            name = "story_genres",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "story_authors",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wordIndex asc")
    private List<StoryWord> words = new ArrayList<>();

    protected Story() {
    }

    public Story(
            UUID id,
            String title,
            String normalizedTitle,
            String content,
            StoryStatus status,
            UserAccount createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.normalizedTitle = normalizedTitle;
        this.content = content;
        this.status = status;
        this.version = 1;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public StoryStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public List<Genre> getGenres() {
        return genres.stream()
                .sorted(Comparator.comparing(Genre::getName))
                .toList();
    }

    public List<Author> getAuthors() {
        return authors.stream()
                .sorted(Comparator.comparing(Author::getName))
                .toList();
    }

    public void update(
            String title,
            String normalizedTitle,
            String content,
            StoryStatus status,
            Instant updatedAt
    ) {
        this.title = title;
        this.normalizedTitle = normalizedTitle;
        this.content = content;
        this.status = status;
        this.version++;
        this.updatedAt = updatedAt;
    }

    public void archive(Instant updatedAt) {
        this.status = StoryStatus.ARCHIVED;
        this.version++;
        this.updatedAt = updatedAt;
    }

    public void replaceGenres(List<Genre> genres) {
        this.genres.clear();
        this.genres.addAll(genres);
    }

    public void replaceAuthors(List<Author> authors) {
        this.authors.clear();
        this.authors.addAll(authors);
    }

    public void replaceWords(List<StoryWord> words) {
        this.words.clear();
        this.words.addAll(words);
    }
}
