package com.lexease.auth;

import com.lexease.shared.api.ApiException;
import com.lexease.users.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthProperties authProperties,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public CreatedRefreshToken create(UserAccount user, String deviceId) {
        Instant now = Instant.now(clock);
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                hash(rawToken),
                deviceId,
                now.plus(authProperties.refreshToken().ttl()),
                now);
        refreshTokenRepository.save(refreshToken);
        return new CreatedRefreshToken(rawToken, refreshToken);
    }

    public RefreshToken consumeForRotation(String rawToken) {
        Instant now = Instant.now(clock);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        if (!refreshToken.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }
        refreshToken.revoke(now);
        return refreshToken;
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.revoke(Instant.now(clock));
                    }
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    authProperties.refreshToken().pepper().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] digest = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    public record CreatedRefreshToken(String rawToken, RefreshToken entity) {
    }
}
