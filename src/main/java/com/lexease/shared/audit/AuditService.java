package com.lexease.shared.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    public void log(UUID actorUserId, String action, String targetType, UUID targetId) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(),
                actorUserId,
                action,
                targetType,
                targetId,
                Map.of(),
                Instant.now(clock)));
    }
}
