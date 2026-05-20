package com.lexease.reading.dtos.req;

import com.lexease.reading.ReadingSessionMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StartReadingSessionRequest(
        @NotNull UUID storyId,
        @Size(max = 100) String voice,
        ReadingSessionMode mode
) {
    public ReadingSessionMode resolvedMode() {
        return mode == null ? ReadingSessionMode.RESUME_OR_START : mode;
    }
}
