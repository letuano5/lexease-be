package com.lexease.notifications;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestPushSender implements PushSender {
    private final DeviceTokenRepository deviceTokenRepository;
    private Map<String, String> lastData = Map.of();
    private UUID lastUserId;
    private boolean nextSendInvalidatesTokens;

    public TestPushSender(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public PushSendResult send(UUID userId, String title, String body, Map<String, String> data) {
        this.lastUserId = userId;
        this.lastData = Map.copyOf(data);
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            return PushSendResult.failed(0, List.of(), "No active device tokens");
        }
        if (nextSendInvalidatesTokens) {
            nextSendInvalidatesTokens = false;
            return PushSendResult.failed(
                    tokens.size(),
                    tokens.stream().map(DeviceToken::getId).toList(),
                    "UNREGISTERED");
        }
        return PushSendResult.success(tokens.size(), tokens.size(), List.of());
    }

    public Map<String, String> getLastData() {
        return lastData;
    }

    public UUID getLastUserId() {
        return lastUserId;
    }

    public void invalidateNextSendTokens() {
        this.nextSendInvalidatesTokens = true;
    }
}
