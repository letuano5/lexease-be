package com.lexease.guardians;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianChildLinkRepository extends JpaRepository<GuardianChildLink, UUID> {
    boolean existsByGuardianIdAndChildIdAndStatusNot(UUID guardianId, UUID childId, GuardianChildLinkStatus status);

    boolean existsByGuardianIdAndChildIdAndStatus(UUID guardianId, UUID childId, GuardianChildLinkStatus status);

    boolean existsByChildIdAndStatus(UUID childId, GuardianChildLinkStatus status);

    @EntityGraph(attributePaths = {"guardian", "child"})
    List<GuardianChildLink> findByGuardianIdAndStatusOrderByCreatedAtDesc(
            UUID guardianId,
            GuardianChildLinkStatus status
    );

    @EntityGraph(attributePaths = {"guardian", "child"})
    List<GuardianChildLink> findByChildIdOrderByCreatedAtDesc(UUID childId);

    @EntityGraph(attributePaths = {"guardian", "child"})
    List<GuardianChildLink> findByGuardianIdOrChildId(UUID guardianId, UUID childId);
}
