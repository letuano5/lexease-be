package com.lexease.stories.dtos.req;

import com.lexease.stories.StoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record StoryUpsertRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotNull List<@NotNull UUID> genreIds,
        @NotNull List<@NotNull UUID> authorIds,
        @NotNull StoryStatus status
) {
}
