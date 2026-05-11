package com.lexease.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lexease.security")
public record AuthProperties(Jwt jwt, RefreshToken refreshToken) {
    public record Jwt(String issuer, String accessSecret, Duration accessTokenTtl) {
    }

    public record RefreshToken(String pepper, Duration ttl) {
    }
}
