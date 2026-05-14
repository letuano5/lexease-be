package com.lexease.genres.dtos.res;

import com.lexease.genres.Genre;
import java.util.UUID;

public record GenreResponse(UUID id, String name) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName());
    }
}
