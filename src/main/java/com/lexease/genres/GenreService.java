package com.lexease.genres;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.audit.AuditAction;
import com.lexease.shared.audit.AuditService;
import com.lexease.shared.audit.AuditTargetType;
import com.lexease.genres.dtos.req.GenreUpsertRequest;
import com.lexease.genres.dtos.res.GenreResponse;
import com.lexease.shared.text.TextNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreService {
    private final GenreRepository genreRepository;
    private final AuditService auditService;
    private final Clock clock;

    public GenreService(
            GenreRepository genreRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.genreRepository = genreRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> list() {
        return genreRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(GenreResponse::from)
                .toList();
    }

    @Transactional
    public GenreResponse create(UUID actorId, GenreUpsertRequest request) {
        String name = request.name().trim();
        String normalizedName = TextNormalizer.normalizeForSearch(name);
        genreRepository.findByNormalizedName(normalizedName)
                .ifPresent(genre -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.GENRE_ALREADY_EXISTS, "Genre already exists");
        });
        Instant now = Instant.now(clock);
        Genre genre = genreRepository.save(new Genre(UUID.randomUUID(), name, normalizedName, now, now));
        auditService.log(actorId, AuditAction.GENRE_CREATED, AuditTargetType.GENRE, genre.getId());
        return GenreResponse.from(genre);
    }

    @Transactional
    public GenreResponse update(UUID actorId, UUID id, GenreUpsertRequest request) {
        Genre genre = load(id);
        String name = request.name().trim();
        String normalizedName = TextNormalizer.normalizeForSearch(name);
        genreRepository.findByNormalizedName(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.GENRE_ALREADY_EXISTS, "Genre already exists");
        });
        genre.update(name, normalizedName, Instant.now(clock));
        genre = genreRepository.save(genre);
        auditService.log(actorId, AuditAction.GENRE_UPDATED, AuditTargetType.GENRE, genre.getId());
        return GenreResponse.from(genre);
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        Genre genre = load(id);
        genre.delete(Instant.now(clock));
        genreRepository.save(genre);
        auditService.log(actorId, AuditAction.GENRE_DELETED, AuditTargetType.GENRE, genre.getId());
    }

    private Genre load(UUID id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.GENRE_NOT_FOUND, "Genre not found"));
        if (genre.isDeleted()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.GENRE_NOT_FOUND, "Genre not found");
        }
        return genre;
    }
}
