package com.lexease.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, UUID> {
    List<ReminderSchedule> findByChildIdOrderByLocalTime(UUID childId);

    @Query(value = """
            select *
            from reminder_schedules
            where enabled = true
              and next_run_at is not null
              and next_run_at <= :now
            order by next_run_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<ReminderSchedule> claimDueSchedules(@Param("now") Instant now, @Param("limit") int limit);
}
