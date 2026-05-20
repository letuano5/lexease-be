package com.lexease.tts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TtsAssetRepository extends JpaRepository<TtsAsset, UUID> {
    Optional<TtsAsset> findByStoryIdAndStoryVersionAndVoiceId(UUID storyId, int storyVersion, String voiceId);

    List<TtsAsset> findByStoryIdAndStoryVersionOrderByUpdatedAtDesc(UUID storyId, int storyVersion);
}
