package com.lexease.reading.dtos.req;

import com.lexease.reading.ReadingEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ReadingProgressEventRequest(
        @NotNull ReadingEventType type,
        @Size(max = 200) String word,
        Integer wordIndex,
        Long timestampMs,
        Map<String, Object> metadata
) {
}
