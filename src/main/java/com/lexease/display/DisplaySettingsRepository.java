package com.lexease.display;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisplaySettingsRepository extends JpaRepository<DisplaySettings, UUID> {
    Optional<DisplaySettings> findByChildId(UUID childId);
}
