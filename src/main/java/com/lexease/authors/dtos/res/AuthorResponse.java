package com.lexease.authors.dtos.res;

import com.lexease.authors.Author;
import java.util.UUID;

public record AuthorResponse(UUID id, String name) {
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName());
    }
}
