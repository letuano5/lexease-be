package com.lexease.authors;

import com.lexease.shared.security.UserPrincipal;
import com.lexease.authors.dtos.req.AuthorUpsertRequest;
import com.lexease.authors.dtos.res.AuthorResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController {
    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/authors")
    List<AuthorResponse> list() {
        return authorService.list();
    }

    @PostMapping("/authors")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    AuthorResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthorUpsertRequest request
    ) {
        return authorService.create(principal.id(), request);
    }

    @PatchMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    AuthorResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuthorUpsertRequest request
    ) {
        return authorService.update(principal.id(), id, request);
    }

    @DeleteMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        authorService.delete(principal.id(), id);
    }
}
