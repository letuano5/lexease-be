package com.lexease.storage;

import java.net.URI;

public interface ObjectStorageService {
    String putObject(String objectKey, byte[] content, String mimeType);

    URI getSignedReadUrl(String objectKey);

    StoredObject readSignedObject(String objectKey, long expiresAtEpochSecond, String signature);
}
