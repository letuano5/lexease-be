package com.lexease.notifications;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FirebasePushSender implements PushSender {
    private static final Logger logger = LoggerFactory.getLogger(FirebasePushSender.class);

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    public FirebasePushSender(FirebaseMessaging firebaseMessaging, DeviceTokenRepository deviceTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public PushSendResult send(UUID userId, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            return PushSendResult.failed(0, List.of(), "No active device tokens");
        }
        int sent = 0;
        List<UUID> invalidTokenIds = new ArrayList<>();
        String failureReason = null;
        for (DeviceToken token : tokens) {
            Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();
            try {
                firebaseMessaging.send(message);
                sent++;
            } catch (FirebaseMessagingException ex) {
                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    invalidTokenIds.add(token.getId());
                }
                failureReason = ex.getMessagingErrorCode() == null
                        ? ex.getMessage()
                        : ex.getMessagingErrorCode().name();
                logger.warn("Could not send notification to token {} for user {}: {}", token.getId(), userId, failureReason);
            }
        }
        if (sent > 0) {
            return PushSendResult.success(tokens.size(), sent, invalidTokenIds);
        }
        return PushSendResult.failed(tokens.size(), invalidTokenIds, failureReason == null ? "Could not send notification" : failureReason);
    }
}
