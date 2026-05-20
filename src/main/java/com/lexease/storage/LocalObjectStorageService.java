package com.lexease.storage;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocalObjectStorageService implements ObjectStorageService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectStorageProperties properties;
    private final Clock clock;

    public LocalObjectStorageService(ObjectStorageProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String putObject(String objectKey, byte[] content, String mimeType) {
        Path target = resolveObjectPath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            Files.writeString(target.resolveSibling(target.getFileName() + ".content-type"), mimeType, StandardCharsets.UTF_8);
            return objectKey;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.TTS_GENERATION_FAILED, "Could not store audio");
        }
    }

    @Override
    public URI getSignedReadUrl(String objectKey) {
        long expiresAt = Instant.now(clock).plus(properties.readUrlTtl()).getEpochSecond();
        String signature = sign(objectKey + "." + expiresAt);
        String query = "key=" + encode(objectKey)
                + "&expires=" + expiresAt
                + "&signature=" + signature;
        return URI.create(properties.publicBaseUrl() + "/storage/local?" + query);
    }

    @Override
    public StoredObject readSignedObject(String objectKey, long expiresAtEpochSecond, String signature) {
        if (expiresAtEpochSecond < Instant.now(clock).getEpochSecond()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Signed URL expired");
        }
        String expected = sign(objectKey + "." + expiresAtEpochSecond);
        if (!constantTimeEquals(expected, signature)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Invalid signed URL");
        }
        Path objectPath = resolveObjectPath(objectKey);
        try {
            if (!Files.isRegularFile(objectPath)) {
                throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.TTS_ASSET_NOT_FOUND, "Object not found");
            }
            String mimeType = Files.exists(objectPath.resolveSibling(objectPath.getFileName() + ".content-type"))
                    ? Files.readString(objectPath.resolveSibling(objectPath.getFileName() + ".content-type")).trim()
                    : "application/octet-stream";
            return new StoredObject(objectKey, mimeType, Files.readAllBytes(objectPath));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.TTS_ASSET_NOT_FOUND, "Could not read object");
        }
    }

    private Path resolveObjectPath(String objectKey) {
        Path root = properties.localRoot().toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Invalid object key");
        }
        return target;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.signingSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign storage URL", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null || expected.length() != actual.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        return result == 0;
    }
}
