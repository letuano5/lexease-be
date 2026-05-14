package com.lexease.display;

import com.lexease.users.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "display_settings")
public class DisplaySettings {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false, unique = true)
    private UserAccount child;

    @Column(name = "font_family", nullable = false)
    private String fontFamily;

    @Column(name = "font_size", nullable = false)
    private int fontSize;

    @Column(name = "line_height", nullable = false)
    private BigDecimal lineHeight;

    @Column(name = "letter_spacing", nullable = false)
    private BigDecimal letterSpacing;

    @Column(name = "background_color", nullable = false)
    private String backgroundColor;

    @Column(name = "text_color", nullable = false)
    private String textColor;

    @Column(name = "theme_name")
    private String themeName;

    @Column(name = "settings_version", nullable = false)
    private int settingsVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DisplaySettings() {
    }

    public DisplaySettings(
            UUID id,
            UserAccount child,
            DisplaySettingsDefaults defaults,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.child = child;
        this.settingsVersion = 1;
        this.createdAt = createdAt;
        apply(defaults);
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getChild() {
        return child;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public BigDecimal getLineHeight() {
        return lineHeight;
    }

    public BigDecimal getLetterSpacing() {
        return letterSpacing;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getThemeName() {
        return themeName;
    }

    public int getSettingsVersion() {
        return settingsVersion;
    }

    public void save(
            String fontFamily,
            int fontSize,
            BigDecimal lineHeight,
            BigDecimal letterSpacing,
            String backgroundColor,
            String textColor,
            String themeName,
            Instant updatedAt,
            boolean incrementVersion
    ) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.lineHeight = lineHeight;
        this.letterSpacing = letterSpacing;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.themeName = themeName;
        if (incrementVersion) {
            this.settingsVersion++;
        }
        this.updatedAt = updatedAt;
    }

    public void reset(DisplaySettingsDefaults defaults, Instant updatedAt) {
        apply(defaults);
        this.settingsVersion++;
        this.updatedAt = updatedAt;
    }

    private void apply(DisplaySettingsDefaults defaults) {
        this.fontFamily = defaults.fontFamily();
        this.fontSize = defaults.fontSize();
        this.lineHeight = defaults.lineHeight();
        this.letterSpacing = defaults.letterSpacing();
        this.backgroundColor = defaults.backgroundColor();
        this.textColor = defaults.textColor();
        this.themeName = defaults.themeName();
    }
}
