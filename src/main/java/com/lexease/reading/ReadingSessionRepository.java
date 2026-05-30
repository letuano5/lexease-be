package com.lexease.reading;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, UUID> {
    Optional<ReadingSession> findFirstByChildIdAndStoryIdAndVoiceIdAndStatusOrderByLastActiveAtDesc(
            UUID childId,
            UUID storyId,
            String voiceId,
            ReadingSessionStatus status);

    List<ReadingSession> findByChildIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            UUID childId,
            Instant start,
            Instant end);
}
