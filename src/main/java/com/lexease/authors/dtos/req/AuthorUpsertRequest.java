package com.lexease.authors.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorUpsertRequest(
        @NotBlank @Size(max = 200) String name
) {
}
