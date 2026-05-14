package com.lexease.genres.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreUpsertRequest(
        @NotBlank @Size(max = 100) String name
) {
}
