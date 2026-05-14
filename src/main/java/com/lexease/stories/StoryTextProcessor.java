package com.lexease.stories;

import com.lexease.shared.text.TextNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StoryTextProcessor {
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    public String normalizeForSearch(String value) {
        return TextNormalizer.normalizeForSearch(value);
    }

    public List<ProcessedWord> splitWords(String content) {
        List<ProcessedWord> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(content);
        while (matcher.find()) {
            String text = matcher.group();
            words.add(new ProcessedWord(
                    words.size(),
                    text,
                    normalizeForSearch(text),
                    matcher.start(),
                    matcher.end()));
        }
        return words;
    }

    public record ProcessedWord(
            int wordIndex,
            String text,
            String normalizedText,
            int startChar,
            int endChar
    ) {
    }
}
