package com.lexease.scoring;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lexease.scoring")
public record ScoringProperties(
        String baseUrl,
        ScoringProviderMode mode,
        String provider,
        String model,
        String promptVersion,
        String callbackBaseUrl,
        Duration recordingRetention
) {
    public ScoringProviderMode mode() {
        return mode == null ? ScoringProviderMode.ASYNC : mode;
    }

    public String provider() {
        return provider == null || provider.isBlank() ? "gemini" : provider;
    }

    public String model() {
        return model == null || model.isBlank() ? "gemini-configured-model" : model;
    }

    public String promptVersion() {
        return promptVersion == null || promptVersion.isBlank() ? "reading-evaluation-v1" : promptVersion;
    }

    public String callbackBaseUrl() {
        return callbackBaseUrl == null || callbackBaseUrl.isBlank() ? "http://localhost:8080" : callbackBaseUrl;
    }

    public Duration recordingRetention() {
        return recordingRetention == null ? Duration.ofDays(30) : recordingRetention;
    }
}
