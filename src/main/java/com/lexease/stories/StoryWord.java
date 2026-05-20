package com.lexease.stories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "story_words")
public class StoryWord {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "word_index", nullable = false)
    private int wordIndex;

    @Column(nullable = false)
    private String text;

    @Column(name = "normalized_text", nullable = false)
    private String normalizedText;

    @Column(name = "start_char", nullable = false)
    private int startChar;

    @Column(name = "end_char", nullable = false)
    private int endChar;

    protected StoryWord() {
    }

    public StoryWord(
            UUID id,
            Story story,
            int wordIndex,
            String text,
            String normalizedText,
            int startChar,
            int endChar
    ) {
        this.id = id;
        this.story = story;
        this.wordIndex = wordIndex;
        this.text = text;
        this.normalizedText = normalizedText;
        this.startChar = startChar;
        this.endChar = endChar;
    }

    public UUID getId() {
        return id;
    }

    public int getWordIndex() {
        return wordIndex;
    }

    public String getText() {
        return text;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public int getStartChar() {
        return startChar;
    }

    public int getEndChar() {
        return endChar;
    }
}
