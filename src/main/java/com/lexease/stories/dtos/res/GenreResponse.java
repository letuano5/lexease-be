package com.lexease.stories.dtos.res;

import com.lexease.stories.Genre;
import java.util.UUID;

public record GenreResponse(UUID id, String name) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName());
    }
}
