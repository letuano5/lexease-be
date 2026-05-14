package com.lexease.authors;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.audit.AuditAction;
import com.lexease.shared.audit.AuditService;
import com.lexease.shared.audit.AuditTargetType;
import com.lexease.authors.dtos.req.AuthorUpsertRequest;
import com.lexease.authors.dtos.res.AuthorResponse;
import com.lexease.shared.text.TextNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuditService auditService;
    private final Clock clock;

    public AuthorService(
            AuthorRepository authorRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.authorRepository = authorRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AuthorResponse> list() {
        return authorRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(AuthorResponse::from)
                .toList();
    }

    @Transactional
    public AuthorResponse create(UUID actorId, AuthorUpsertRequest request) {
        String name = request.name().trim();
        String normalizedName = TextNormalizer.normalizeForSearch(name);
        authorRepository.findByNormalizedName(normalizedName)
                .ifPresent(author -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.AUTHOR_ALREADY_EXISTS, "Author already exists");
        });
        Instant now = Instant.now(clock);
        Author author = authorRepository.save(new Author(UUID.randomUUID(), name, normalizedName, now, now));
        auditService.log(actorId, AuditAction.AUTHOR_CREATED, AuditTargetType.AUTHOR, author.getId());
        return AuthorResponse.from(author);
    }

    @Transactional
    public AuthorResponse update(UUID actorId, UUID id, AuthorUpsertRequest request) {
        Author author = load(id);
        String name = request.name().trim();
        String normalizedName = TextNormalizer.normalizeForSearch(name);
        authorRepository.findByNormalizedName(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.AUTHOR_ALREADY_EXISTS, "Author already exists");
        });
        author.update(name, normalizedName, Instant.now(clock));
        author = authorRepository.save(author);
        auditService.log(actorId, AuditAction.AUTHOR_UPDATED, AuditTargetType.AUTHOR, author.getId());
        return AuthorResponse.from(author);
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        Author author = load(id);
        author.delete(Instant.now(clock));
        authorRepository.save(author);
        auditService.log(actorId, AuditAction.AUTHOR_DELETED, AuditTargetType.AUTHOR, author.getId());
    }

    private Author load(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.AUTHOR_NOT_FOUND, "Author not found"));
        if (author.isDeleted()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.AUTHOR_NOT_FOUND, "Author not found");
        }
        return author;
    }
}
