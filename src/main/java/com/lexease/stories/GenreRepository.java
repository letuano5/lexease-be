package com.lexease.stories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    List<Genre> findAllByOrderByNameAsc();
}
