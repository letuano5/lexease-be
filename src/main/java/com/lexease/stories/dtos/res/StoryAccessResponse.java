package com.lexease.stories.dtos.res;

import java.util.UUID;

public record StoryAccessResponse(
        UUID childId,
        UUID storyId,
        boolean blocked
) {
}
