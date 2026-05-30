package com.lexease.display.dtos.res;

import com.lexease.display.DisplaySettings;
import com.lexease.display.DisplaySettingsDefaults;
import java.math.BigDecimal;
import java.util.UUID;

public record DisplaySettingsResponse(
        UUID childId,
        String fontFamily,
        int fontSize,
        BigDecimal lineHeight,
        BigDecimal letterSpacing,
        String backgroundColor,
        String textColor,
        String highlightBackgroundColor,
        String highlightTextColor,
        String themeName,
        int settingsVersion
) {
    public static DisplaySettingsResponse from(DisplaySettings settings) {
        return new DisplaySettingsResponse(
                settings.getChild().getId(),
                settings.getFontFamily(),
                settings.getFontSize(),
                settings.getLineHeight(),
                settings.getLetterSpacing(),
                settings.getBackgroundColor(),
                settings.getTextColor(),
                settings.getHighlightBackgroundColor(),
                settings.getHighlightTextColor(),
                settings.getThemeName(),
                settings.getSettingsVersion());
    }

    public static DisplaySettingsResponse defaults(UUID childId, DisplaySettingsDefaults defaults) {
        return new DisplaySettingsResponse(
                childId,
                defaults.fontFamily(),
                defaults.fontSize(),
                defaults.lineHeight(),
                defaults.letterSpacing(),
                defaults.backgroundColor(),
                defaults.textColor(),
                defaults.highlightBackgroundColor(),
                defaults.highlightTextColor(),
                defaults.themeName(),
                1);
    }
}
