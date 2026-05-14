package com.lexease.shared.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    public static String normalizeForSearch(String value) {
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "");
        return MULTIPLE_SPACES.matcher(decomposed.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}
