package com.lexease.stories.dtos.res;

import com.lexease.authors.dtos.res.AuthorResponse;
import com.lexease.genres.dtos.res.GenreResponse;
import com.lexease.stories.Story;
import com.lexease.stories.StoryStatus;
import java.util.List;
import java.util.UUID;

public record StorySummaryResponse(
        UUID id,
        String title,
        List<GenreResponse> genres,
        List<AuthorResponse> authors,
        StoryStatus status,
        boolean isBlockedForCurrentChild
) {
    public static StorySummaryResponse from(Story story, boolean isBlockedForCurrentChild) {
        return new StorySummaryResponse(
                story.getId(),
                story.getTitle(),
                story.getGenres().stream().map(GenreResponse::from).toList(),
                story.getAuthors().stream().map(AuthorResponse::from).toList(),
                story.getStatus(),
                isBlockedForCurrentChild);
    }
}
