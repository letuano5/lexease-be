package com.lexease.tts;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lexease.shared.api.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class TtsCallbackVerifierTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");
    private static final String SECRET = "test-tts-callback-secret";

    @Test
    void acceptsTimestampBodySignature() {
        TtsCallbackVerifier verifier = verifier();
        String body = "{\"requestId\":\"asset-001\"}";
        String timestamp = Long.toString(NOW.getEpochSecond());
        String signature = "sha256=" + sign(timestamp + "." + body);

        assertThatCode(() -> verifier.verify(timestamp, signature, body)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidSignature() {
        assertThatThrownBy(() -> verifier().verify(
                Long.toString(NOW.getEpochSecond()),
                "sha256=invalid",
                "{\"requestId\":\"asset-001\"}"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid callback signature");
    }

    @Test
    void rejectsStaleTimestamp() {
        String body = "{}";
        String timestamp = Long.toString(NOW.minus(Duration.ofMinutes(10)).getEpochSecond());
        String signature = "sha256=" + sign(timestamp + "." + body);

        assertThatThrownBy(() -> verifier().verify(timestamp, signature, body))
                .isInstanceOf(ApiException.class)
                .hasMessage("Stale callback timestamp");
    }

    private TtsCallbackVerifier verifier() {
        return new TtsCallbackVerifier(
                new TtsProperties(
                        null,
                        null,
                        SECRET,
                        TtsProviderMode.ASYNC,
                        "vieneu-tts",
                        "Binh",
                        "wav",
                        "vi-VN",
                        Duration.ofMinutes(5),
                        0.10,
                        false),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
