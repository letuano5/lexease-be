package com.lexease.genres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    List<Genre> findAllByDeletedAtIsNullOrderByNameAsc();

    List<Genre> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);

    Optional<Genre> findByNormalizedName(String normalizedName);
}
