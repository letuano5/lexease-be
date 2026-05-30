package com.lexease.scoring.provider;

import java.net.URI;

public record ScoringAudioPayload(
        URI audioUrl,
        String mimeType,
        Long durationMs
) {
}
