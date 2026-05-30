package com.lexease.reading;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingEventRepository extends JpaRepository<ReadingEvent, UUID> {
    long countBySessionId(UUID sessionId);

    @Query("""
            select count(e) from ReadingEvent e
            where e.session.child.id = :childId
              and e.eventType = :eventType
              and e.createdAt >= :start
              and e.createdAt < :end
            """)
    long countByChildAndTypeAndCreatedAtRange(
            @Param("childId") UUID childId,
            @Param("eventType") ReadingEventType eventType,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
