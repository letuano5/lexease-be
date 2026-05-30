package com.lexease.scoring;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.tts.TtsProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScoringCallbackVerifier {
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final TtsProperties ttsProperties;
    private final Clock clock;

    public ScoringCallbackVerifier(TtsProperties ttsProperties, Clock clock) {
        this.ttsProperties = ttsProperties;
        this.clock = clock;
    }

    public void verify(String timestampHeader, String signatureHeader, String rawBody) {
        if (timestampHeader == null || signatureHeader == null) {
            throw invalid("Missing callback signature");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            throw invalid("Invalid callback timestamp");
        }
        long now = Instant.now(clock).getEpochSecond();
        if (Math.abs(now - timestamp) > ttsProperties.callbackTimestampTolerance().toSeconds()) {
            throw invalid("Stale callback timestamp");
        }
        String expected = SIGNATURE_PREFIX + sign(timestamp + "." + rawBody);
        if (!constantTimeEquals(expected, signatureHeader)) {
            throw invalid("Invalid callback signature");
        }
    }

    private String sign(String value) {
        String secret = ttsProperties.callbackSecret();
        if (secret == null || secret.isBlank()) {
            throw invalid("Callback secret is not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw invalid("Could not verify callback signature");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null || expected.length() != actual.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        return result == 0;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_SCORING_CALLBACK, message);
    }
}
