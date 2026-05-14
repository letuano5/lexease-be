package com.lexease.stories;

import com.lexease.guardians.GuardianChildLinkStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryAccessBlockRepository extends JpaRepository<StoryAccessBlock, UUID> {
    Optional<StoryAccessBlock> findByBlockedByGuardianIdAndChildIdAndStoryIdAndActiveTrue(
            UUID guardianId,
            UUID childId,
            UUID storyId);

    List<StoryAccessBlock> findByChildIdAndStoryIdAndActiveTrue(UUID childId, UUID storyId);

    @Query("""
            select count(b) > 0
            from StoryAccessBlock b
            where b.child.id = :childId
              and b.story.id = :storyId
              and b.active = true
              and exists (
                  select 1
                  from GuardianChildLink l
                  where l.guardian.id = b.blockedByGuardian.id
                    and l.child.id = :childId
                    and l.status = :acceptedStatus
              )
            """)
    boolean existsAcceptedActiveBlock(
            @Param("childId") UUID childId,
            @Param("storyId") UUID storyId,
            @Param("acceptedStatus") GuardianChildLinkStatus acceptedStatus);
}
