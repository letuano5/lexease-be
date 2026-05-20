package com.lexease.tts;

import com.lexease.stories.StoryWord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tts_word_timings")
public class TtsWordTiming {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tts_asset_id", nullable = false)
    private TtsAsset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_word_id")
    private StoryWord storyWord;

    @Column(name = "word_index", nullable = false)
    private int wordIndex;

    @Column(nullable = false)
    private String text;

    @Column(name = "start_ms", nullable = false)
    private int startMs;

    @Column(name = "end_ms", nullable = false)
    private int endMs;

    @Column(name = "start_char")
    private Integer startChar;

    @Column(name = "end_char")
    private Integer endChar;

    protected TtsWordTiming() {
    }

    public TtsWordTiming(UUID id, TtsAsset asset, StoryWord storyWord, String text, int startMs, int endMs) {
        this.id = id;
        this.asset = asset;
        this.storyWord = storyWord;
        this.wordIndex = storyWord.getWordIndex();
        this.text = text;
        this.startMs = startMs;
        this.endMs = endMs;
        this.startChar = storyWord.getStartChar();
        this.endChar = storyWord.getEndChar();
    }

    public int getWordIndex() {
        return wordIndex;
    }

    public String getText() {
        return text;
    }

    public int getStartMs() {
        return startMs;
    }

    public int getEndMs() {
        return endMs;
    }

    public Integer getStartChar() {
        return startChar;
    }

    public Integer getEndChar() {
        return endChar;
    }
}
