package com.lexease.notifications;

import java.util.List;
import java.util.UUID;

public record PushSendResult(
        int attempted,
        int sent,
        List<UUID> invalidTokenIds,
        String failureReason
) {
    public static PushSendResult success(int attempted, int sent, List<UUID> invalidTokenIds) {
        return new PushSendResult(attempted, sent, invalidTokenIds, null);
    }

    public static PushSendResult failed(int attempted, List<UUID> invalidTokenIds, String failureReason) {
        return new PushSendResult(attempted, 0, invalidTokenIds, failureReason);
    }
}
