package com.lexease.notifications;

import java.util.Map;
import java.util.UUID;

public interface PushSender {
    PushSendResult send(UUID userId, String title, String body, Map<String, String> data);
}
