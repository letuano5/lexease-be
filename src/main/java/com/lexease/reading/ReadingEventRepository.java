package com.lexease.reading;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingEventRepository extends JpaRepository<ReadingEvent, UUID> {
    long countBySessionId(UUID sessionId);
}
