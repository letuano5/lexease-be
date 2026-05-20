package com.lexease.tts;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lexease.tts")
public record TtsProperties(
        String baseUrl,
        String callbackBaseUrl,
        String callbackSecret,
        TtsProviderMode mode,
        String provider,
        String defaultVoice,
        String audioFormat,
        String language,
        Duration callbackTimestampTolerance,
        double maxWordCountMismatchRatio,
        Boolean autoGenerateOnPublish
) {
    public TtsProviderMode mode() {
        return mode == null ? TtsProviderMode.ASYNC : mode;
    }

    public String provider() {
        return provider == null || provider.isBlank() ? "vieneu-tts" : provider;
    }

    public String defaultVoice() {
        return defaultVoice == null || defaultVoice.isBlank() ? "Binh" : defaultVoice;
    }

    public String audioFormat() {
        return audioFormat == null || audioFormat.isBlank() ? "wav" : audioFormat;
    }

    public String language() {
        return language == null || language.isBlank() ? "vi-VN" : language;
    }

    public Duration callbackTimestampTolerance() {
        return callbackTimestampTolerance == null ? Duration.ofMinutes(5) : callbackTimestampTolerance;
    }

    public double maxWordCountMismatchRatio() {
        return maxWordCountMismatchRatio <= 0 ? 0.10 : maxWordCountMismatchRatio;
    }

    public boolean isAutoGenerateOnPublish() {
        return autoGenerateOnPublish == null || autoGenerateOnPublish;
    }
}
