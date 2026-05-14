package com.lexease.stories.dtos.res;

import com.lexease.stories.Author;
import java.util.UUID;

public record AuthorResponse(UUID id, String name) {
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName());
    }
}
