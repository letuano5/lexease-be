package com.lexease.recordings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordingRepository extends JpaRepository<Recording, UUID> {
    Optional<Recording> findByIdAndDeletedAtIsNull(UUID id);

    List<Recording> findByChildIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID childId,
            Instant start,
            Instant end);

    List<Recording> findBySessionIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID sessionId);
}
