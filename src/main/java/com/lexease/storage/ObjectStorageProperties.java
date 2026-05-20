package com.lexease.storage;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lexease.object-storage")
public record ObjectStorageProperties(
        String mode,
        Path localRoot,
        String publicBaseUrl,
        String signingSecret,
        Duration readUrlTtl
) {
    public Path localRoot() {
        return localRoot == null ? Path.of("build/lexease-storage") : localRoot;
    }

    public String publicBaseUrl() {
        return publicBaseUrl == null || publicBaseUrl.isBlank() ? "" : publicBaseUrl;
    }

    public String signingSecret() {
        return signingSecret == null || signingSecret.isBlank() ? "dev-only-storage-signing-secret" : signingSecret;
    }

    public Duration readUrlTtl() {
        return readUrlTtl == null ? Duration.ofMinutes(15) : readUrlTtl;
    }
}
