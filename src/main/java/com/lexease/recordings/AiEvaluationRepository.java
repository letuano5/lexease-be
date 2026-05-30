package com.lexease.recordings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {
    Optional<AiEvaluation> findByIdAndDeletedAtIsNull(UUID id);

    Optional<AiEvaluation> findFirstByRecordingIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID recordingId);

    @Query("""
            select e from AiEvaluation e
            join e.recording r
            where r.child.id = :childId
              and r.deletedAt is null
              and e.deletedAt is null
              and e.createdAt >= :start
              and e.createdAt < :end
            order by e.createdAt desc
            """)
    List<AiEvaluation> findByChildAndCreatedAtRange(
            @Param("childId") UUID childId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
