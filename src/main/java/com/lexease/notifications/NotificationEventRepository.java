package com.lexease.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {
    boolean existsByScheduleIdAndScheduledFor(UUID scheduleId, Instant scheduledFor);

    @Query(value = """
            select *
            from notification_events
            where status = 'SCHEDULED'
              and scheduled_for <= :now
            order by scheduled_for
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<NotificationEvent> claimDueEvents(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("""
            update NotificationEvent event
            set event.status = com.lexease.notifications.NotificationStatus.IGNORED,
                event.updatedAt = :now
            where event.status = com.lexease.notifications.NotificationStatus.SENT
              and event.sentAt <= :cutoff
              and event.openedAt is null
              and event.practiceStartedAt is null
            """)
    int markIgnored(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
