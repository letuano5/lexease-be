package com.lexease.authors;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
    List<Author> findAllByDeletedAtIsNullOrderByNameAsc();

    List<Author> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);

    Optional<Author> findByNormalizedName(String normalizedName);
}
