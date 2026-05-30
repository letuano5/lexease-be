package com.lexease.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationJobs {
    private static final Logger logger = LoggerFactory.getLogger(NotificationJobs.class);

    private final NotificationSchedulerService schedulerService;
    private final NotificationProperties properties;

    public NotificationJobs(NotificationSchedulerService schedulerService, NotificationProperties properties) {
        this.schedulerService = schedulerService;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 60_000)
    void planDueReminders() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        int count = schedulerService.planDueReminders();
        if (count > 0) {
            logger.info("Planned {} due reminder schedules", count);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    void dispatchDueEvents() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        int count = schedulerService.dispatchDueEvents();
        if (count > 0) {
            logger.info("Dispatched {} due notification events", count);
        }
    }

    @Scheduled(fixedDelay = 900_000)
    void markIgnoredEvents() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        int count = schedulerService.markIgnoredEvents();
        if (count > 0) {
            logger.info("Marked {} notification events as ignored", count);
        }
    }
}
