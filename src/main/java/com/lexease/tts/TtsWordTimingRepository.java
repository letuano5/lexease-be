package com.lexease.tts;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TtsWordTimingRepository extends JpaRepository<TtsWordTiming, UUID> {
    List<TtsWordTiming> findByAssetIdOrderByWordIndex(UUID assetId);

    long countByAssetId(UUID assetId);

    void deleteByAssetId(UUID assetId);
}
