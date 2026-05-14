package com.lexease.genres;

import com.lexease.shared.security.UserPrincipal;
import com.lexease.genres.dtos.req.GenreUpsertRequest;
import com.lexease.genres.dtos.res.GenreResponse;
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
public class GenreController {
    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping("/genres")
    List<GenreResponse> list() {
        return genreService.list();
    }

    @PostMapping("/genres")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    GenreResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenreUpsertRequest request
    ) {
        return genreService.create(principal.id(), request);
    }

    @PatchMapping("/genres/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    GenreResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody GenreUpsertRequest request
    ) {
        return genreService.update(principal.id(), id, request);
    }

    @DeleteMapping("/genres/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        genreService.delete(principal.id(), id);
    }
}
