package com.lexease.scoring.provider;

import java.util.UUID;

public record ScoringStoryPayload(
        UUID id,
        String title,
        String expectedText
) {
}
