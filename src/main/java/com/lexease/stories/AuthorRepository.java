package com.lexease.stories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
    List<Author> findAllByOrderByNameAsc();
}
