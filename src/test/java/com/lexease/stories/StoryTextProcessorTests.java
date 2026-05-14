package com.lexease.stories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StoryTextProcessorTests {
    private final StoryTextProcessor processor = new StoryTextProcessor();

    @Test
    void normalizesVietnameseTextForSearch() {
        assertThat(processor.normalizeForSearch("  Chú Mèo đi học  "))
                .isEqualTo("chu meo di hoc");
    }

    @Test
    void splitsWordsWithCharacterOffsets() {
        var words = processor.splitWords("Chú mèo đi học. Bạn ấy đọc tốt.");

        assertThat(words)
                .extracting(StoryTextProcessor.ProcessedWord::normalizedText)
                .containsExactly("chu", "meo", "di", "hoc", "ban", "ay", "doc", "tot");
        assertThat(words.getFirst().startChar()).isZero();
        assertThat(words.getFirst().endChar()).isEqualTo(3);
    }
}
