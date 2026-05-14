package com.lexease.stories.dtos.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StoryAccessChangeRequest(
        @NotNull UUID childId,
        @NotNull UUID storyId,
        Boolean blocked,
        @Size(max = 500) String reason
) {
}
