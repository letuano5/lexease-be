package com.lexease.notifications;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class FirebaseConfig {
    @Bean
    FirebaseApp firebaseApp(NotificationProperties properties) throws IOException {
        NotificationProperties.Firebase firebase = properties.firebase();
        if (firebase == null || isBlank(firebase.projectId()) || isBlank(firebase.credentialsPath())) {
            throw new IllegalStateException("Firebase project id and credentials path are required");
        }
        try (FileInputStream credentialsStream = new FileInputStream(firebase.credentialsPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setProjectId(firebase.projectId())
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
