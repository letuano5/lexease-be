package com.lexease.reading.dtos.res;

import com.lexease.stories.Story;
import java.util.UUID;

public record ReadingStoryResponse(
        UUID id,
        String title,
        String content
) {
    public static ReadingStoryResponse from(Story story) {
        return new ReadingStoryResponse(story.getId(), story.getTitle(), story.getContent());
    }
}
