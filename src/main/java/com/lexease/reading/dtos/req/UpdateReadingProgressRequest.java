package com.lexease.reading.dtos.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateReadingProgressRequest(
        @NotNull @Min(0) Integer currentWordIndex,
        @NotNull @Min(0) Long elapsedMs,
        List<@Valid ReadingProgressEventRequest> events
) {
    public List<ReadingProgressEventRequest> resolvedEvents() {
        return events == null ? List.of() : events;
    }
}
