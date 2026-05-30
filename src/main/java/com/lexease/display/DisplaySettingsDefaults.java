package com.lexease.display;

import java.math.BigDecimal;

public record DisplaySettingsDefaults(
        String fontFamily,
        int fontSize,
        BigDecimal lineHeight,
        BigDecimal letterSpacing,
        String backgroundColor,
        String textColor,
        String highlightBackgroundColor,
        String highlightTextColor,
        String themeName
) {
    public static DisplaySettingsDefaults standard() {
        return new DisplaySettingsDefaults(
                "system",
                20,
                new BigDecimal("1.60"),
                new BigDecimal("0.04"),
                "#FFFFFF",
                "#111111",
                "#FEF08A",
                "#111111",
                "default");
    }
}
