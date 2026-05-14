package com.lexease.stories.dtos.res;

import com.lexease.authors.dtos.res.AuthorResponse;
import com.lexease.genres.dtos.res.GenreResponse;
import com.lexease.stories.Story;
import com.lexease.stories.StoryStatus;
import com.lexease.stories.TtsStatus;
import java.util.List;
import java.util.UUID;

public record StoryDetailResponse(
        UUID id,
        String title,
        String content,
        List<GenreResponse> genres,
        List<AuthorResponse> authors,
        StoryStatus status,
        int version,
        TtsStatus ttsStatus
) {
    public static StoryDetailResponse from(Story story) {
        return new StoryDetailResponse(
                story.getId(),
                story.getTitle(),
                story.getContent(),
                story.getGenres().stream().map(GenreResponse::from).toList(),
                story.getAuthors().stream().map(AuthorResponse::from).toList(),
                story.getStatus(),
                story.getVersion(),
                TtsStatus.PENDING);
    }
}
