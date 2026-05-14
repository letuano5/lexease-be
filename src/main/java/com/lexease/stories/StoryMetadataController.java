package com.lexease.stories;

import com.lexease.stories.dtos.res.AuthorResponse;
import com.lexease.stories.dtos.res.GenreResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoryMetadataController {
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;

    public StoryMetadataController(GenreRepository genreRepository, AuthorRepository authorRepository) {
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
    }

    @GetMapping("/genres")
    List<GenreResponse> listGenres() {
        return genreRepository.findAllByOrderByNameAsc().stream()
                .map(GenreResponse::from)
                .toList();
    }

    @GetMapping("/authors")
    List<AuthorResponse> listAuthors() {
        return authorRepository.findAllByOrderByNameAsc().stream()
                .map(AuthorResponse::from)
                .toList();
    }
}
