package com.lexease.stories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoryRepository extends JpaRepository<Story, UUID>, JpaSpecificationExecutor<Story> {
}
