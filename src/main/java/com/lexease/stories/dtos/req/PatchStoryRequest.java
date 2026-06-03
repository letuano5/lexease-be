package com.lexease.stories.dtos.req;

import com.lexease.stories.StoryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PatchStoryRequest(
        @Size(max = 300) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String content,
        List<@NotNull UUID> genreIds,
        List<@NotNull UUID> authorIds,
        StoryStatus status
) {
}
