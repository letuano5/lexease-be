package com.lexease.storage;

public record StoredObject(
        String objectKey,
        String mimeType,
        byte[] content
) {
}
