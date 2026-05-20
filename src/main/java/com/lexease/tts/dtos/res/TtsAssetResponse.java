package com.lexease.tts.dtos.res;

import com.lexease.storage.ObjectStorageService;
import com.lexease.tts.TtsAsset;
import com.lexease.tts.TtsStatus;
import java.net.URI;
import java.util.UUID;

public record TtsAssetResponse(
        UUID assetId,
        UUID storyId,
        int storyVersion,
        String voice,
        TtsStatus status,
        URI audioUrl,
        long wordTimingCount,
        String providerJobId,
        String errorMessage
) {
    public static TtsAssetResponse from(TtsAsset asset, long wordTimingCount, ObjectStorageService storageService) {
        URI audioUrl = asset.getStatus() == TtsStatus.READY && asset.getAudioObjectKey() != null
                ? storageService.getSignedReadUrl(asset.getAudioObjectKey())
                : null;
        return new TtsAssetResponse(
                asset.getId(),
                asset.getStory().getId(),
                asset.getStoryVersion(),
                asset.getVoiceId(),
                asset.getStatus(),
                audioUrl,
                wordTimingCount,
                asset.getProviderJobId(),
                asset.getErrorMessage());
    }
}
