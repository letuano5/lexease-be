package com.lexease.tts;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.storage.ObjectStorageService;
import com.lexease.stories.Story;
import com.lexease.stories.StoryRepository;
import com.lexease.stories.StoryStatus;
import com.lexease.stories.StoryWord;
import com.lexease.tts.dtos.req.TtsCallbackRequest;
import com.lexease.tts.dtos.res.TtsAssetGenerationResult;
import com.lexease.tts.dtos.res.TtsAssetResponse;
import com.lexease.tts.provider.TtsAudio;
import com.lexease.tts.provider.TtsGenerationResult;
import com.lexease.tts.provider.TtsJobSubmitRequest;
import com.lexease.tts.provider.TtsJobSubmitResponse;
import com.lexease.tts.provider.TtsProviderClient;
import com.lexease.tts.provider.TtsProviderError;
import com.lexease.tts.provider.TtsProviderWord;
import com.lexease.tts.provider.TtsSyncRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TtsService {
    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);

    private final TtsAssetRepository assetRepository;
    private final TtsWordTimingRepository timingRepository;
    private final StoryRepository storyRepository;
    private final TtsProviderClient providerClient;
    private final ObjectStorageService objectStorageService;
    private final TtsProperties properties;
    private final Clock clock;

    public TtsService(
            TtsAssetRepository assetRepository,
            TtsWordTimingRepository timingRepository,
            StoryRepository storyRepository,
            TtsProviderClient providerClient,
            ObjectStorageService objectStorageService,
            TtsProperties properties,
            Clock clock
    ) {
        this.assetRepository = assetRepository;
        this.timingRepository = timingRepository;
        this.storyRepository = storyRepository;
        this.providerClient = providerClient;
        this.objectStorageService = objectStorageService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TtsAssetGenerationResult generateAsset(UUID storyId, String requestedVoice, boolean refresh) {
        Story story = findStory(storyId);
        validateStoryEligible(story);
        String voice = resolveVoice(requestedVoice);
        TtsAsset asset = getOrCreateAsset(story, voice);
        if (!refresh && asset.getStatus() == TtsStatus.READY) {
            return new TtsAssetGenerationResult(toResponse(asset), false);
        }
        boolean accepted = startGeneration(asset, story, true);
        return new TtsAssetGenerationResult(toResponse(asset), accepted);
    }

    @Transactional
    public TtsAsset ensureAssetQueued(UUID storyId, String requestedVoice) {
        Story story = findStory(storyId);
        validateStoryEligible(story);
        String voice = resolveVoice(requestedVoice);
        TtsAsset asset = getOrCreateAsset(story, voice);
        if (asset.getStatus() == TtsStatus.PENDING) {
            try {
                startGeneration(asset, story, false);
            } catch (RuntimeException ex) {
                logger.warn("Could not enqueue TTS asset {} for story {}", asset.getId(), story.getId());
            }
        }
        return asset;
    }

    @Transactional
    public void enqueueDefaultAssetForPublishedStory(Story story) {
        if (!properties.isAutoGenerateOnPublish() || story.getStatus() != StoryStatus.PUBLISHED) {
            return;
        }
        TtsAsset asset = getOrCreateAsset(story, properties.defaultVoice());
        if (asset.getStatus() == TtsStatus.READY || asset.getStatus() == TtsStatus.PROCESSING) {
            return;
        }
        try {
            startGeneration(asset, story, false);
        } catch (RuntimeException ex) {
            logger.warn("Could not submit TTS asset {} for story {}: {}", asset.getId(), story.getId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<TtsAssetResponse> listAssets(UUID storyId) {
        Story story = findStory(storyId);
        return assetRepository.findByStoryIdAndStoryVersionOrderByUpdatedAtDesc(story.getId(), story.getVersion()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TtsAssetResponse getAssetResponse(TtsAsset asset) {
        return toResponse(asset);
    }

    @Transactional(readOnly = true)
    public List<TtsWordTiming> getTimings(UUID assetId) {
        return timingRepository.findByAssetIdOrderByWordIndex(assetId);
    }

    @Transactional
    public void handleCallback(UUID requestId, TtsCallbackRequest callback) {
        if (callback.requestId() == null || !requestId.toString().equals(callback.requestId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TTS_CALLBACK, "Callback request id mismatch");
        }
        TtsAsset asset = assetRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.TTS_ASSET_NOT_FOUND, "TTS asset not found"));
        if ("FAILED".equalsIgnoreCase(callback.status())) {
            asset.markFailed(providerErrorMessage(callback.error()), Instant.now(clock));
            return;
        }
        if (!"READY".equalsIgnoreCase(callback.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TTS_CALLBACK, "Unsupported callback status");
        }
        try {
            storeResult(asset, new TtsGenerationResult(
                    callback.requestId(),
                    callback.voice(),
                    callback.language(),
                    callback.audio(),
                    callback.words(),
                    null));
        } catch (ApiException ex) {
            asset.markFailed(ex.getMessage(), Instant.now(clock));
            throw ex;
        }
    }

    private boolean startGeneration(TtsAsset asset, Story story, boolean surfaceProviderErrors) {
        Instant now = Instant.now(clock);
        asset.startProcessing(asset.getId().toString(), now);
        timingRepository.deleteByAssetId(asset.getId());
        if (properties.mode() == TtsProviderMode.SYNC) {
            try {
                TtsGenerationResult result = providerClient.generateSync(new TtsSyncRequest(
                        story.getContent(),
                        asset.getVoiceId(),
                        properties.audioFormat(),
                        properties.language()));
                storeResult(asset, result);
                return false;
            } catch (RuntimeException ex) {
                asset.markFailed("Could not generate TTS audio", Instant.now(clock));
                if (surfaceProviderErrors) {
                    throw providerFailure(ex);
                }
                throw ex;
            }
        }
        try {
            TtsJobSubmitResponse response = providerClient.submitJob(new TtsJobSubmitRequest(
                    asset.getId().toString(),
                    callbackUrl(asset.getId()),
                    story.getContent(),
                    asset.getVoiceId(),
                    properties.audioFormat(),
                    properties.language()));
            asset.markSubmitted(response == null ? null : response.jobId(), Instant.now(clock));
            return true;
        } catch (RuntimeException ex) {
            asset.markFailed("Could not submit TTS job", Instant.now(clock));
            if (surfaceProviderErrors) {
                throw providerFailure(ex);
            }
            throw ex;
        }
    }

    private void storeResult(TtsAsset asset, TtsGenerationResult result) {
        validateResult(asset, result);
        TtsAudio audio = result.audio();
        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(audio.contentBase64());
        } catch (IllegalArgumentException ex) {
            throw invalidProviderResponse("Invalid audio content");
        }
        String objectKey = objectKey(asset, audio.format());
        objectStorageService.putObject(objectKey, audioBytes, audio.mimeType());
        List<StoryWord> storyWords = asset.getStory().getWords();
        List<TtsWordTiming> timings = result.words().stream()
                .sorted(Comparator.comparingInt(TtsProviderWord::index))
                .map(word -> {
                    StoryWord storyWord = storyWords.get(word.index());
                    return new TtsWordTiming(UUID.randomUUID(), asset, storyWord, word.text(), word.startMs(), word.endMs());
                })
                .toList();
        timingRepository.deleteByAssetId(asset.getId());
        timingRepository.saveAll(timings);
        asset.markReady(objectKey, audio.mimeType(), audio.durationMs(), audio.sampleRateHz(), Instant.now(clock));
    }

    private void validateResult(TtsAsset asset, TtsGenerationResult result) {
        if (result == null || result.audio() == null || result.audio().contentBase64() == null || result.audio().contentBase64().isBlank()) {
            throw invalidProviderResponse("TTS response is missing audio");
        }
        if (result.audio().mimeType() == null || result.audio().mimeType().isBlank()) {
            throw invalidProviderResponse("TTS response is missing audio mime type");
        }
        if (result.audio().format() == null || result.audio().format().isBlank()) {
            throw invalidProviderResponse("TTS response is missing audio format");
        }
        List<TtsProviderWord> words = result.words();
        List<StoryWord> storyWords = asset.getStory().getWords();
        if (words == null || words.isEmpty() || storyWords.isEmpty()) {
            throw invalidProviderResponse("TTS response is missing word timings");
        }
        int allowedMismatch = Math.max(2, (int) Math.ceil(storyWords.size() * properties.maxWordCountMismatchRatio()));
        if (Math.abs(storyWords.size() - words.size()) > allowedMismatch) {
            throw invalidProviderResponse("TTS word count does not match story words");
        }
        if (words.size() > storyWords.size()) {
            throw invalidProviderResponse("TTS response has more words than the story");
        }
        int previousStart = -1;
        for (int i = 0; i < words.size(); i++) {
            TtsProviderWord word = words.get(i);
            if (word.index() != i) {
                throw invalidProviderResponse("TTS words must be sorted by index");
            }
            if (word.startMs() < 0 || word.endMs() <= word.startMs() || word.startMs() < previousStart) {
                throw invalidProviderResponse("TTS word timings are invalid");
            }
            previousStart = word.startMs();
        }
    }

    private TtsAsset getOrCreateAsset(Story story, String voice) {
        return assetRepository.findByStoryIdAndStoryVersionAndVoiceId(story.getId(), story.getVersion(), voice)
                .orElseGet(() -> assetRepository.save(new TtsAsset(
                        UUID.randomUUID(),
                        story,
                        story.getVersion(),
                        properties.provider(),
                        voice,
                        Instant.now(clock))));
    }

    private TtsAssetResponse toResponse(TtsAsset asset) {
        return TtsAssetResponse.from(asset, timingRepository.countByAssetId(asset.getId()), objectStorageService);
    }

    private Story findStory(UUID storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found"));
    }

    private void validateStoryEligible(Story story) {
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.STORY_NOT_FOUND, "Story is not published");
        }
        if (story.getContent() == null || story.getContent().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Story content is empty");
        }
    }

    private String resolveVoice(String requestedVoice) {
        return requestedVoice == null || requestedVoice.isBlank() ? properties.defaultVoice() : requestedVoice.trim();
    }

    private String callbackUrl(UUID assetId) {
        String baseUrl = validatedCallbackBaseUrl();
        return baseUrl + "/internal/tts/jobs/" + assetId + "/callback";
    }

    private String validatedCallbackBaseUrl() {
        String baseUrl = properties.callbackBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.TTS_GENERATION_FAILED, "TTS callback base URL is not configured");
        }
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.TTS_GENERATION_FAILED, "TTS callback base URL is invalid");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.TTS_GENERATION_FAILED, "TTS callback base URL must be an absolute HTTP URL");
        }
        return normalized;
    }

    private String objectKey(TtsAsset asset, String format) {
        String extension = format == null || format.isBlank() ? properties.audioFormat() : format;
        String safeVoice = asset.getVoiceId().replaceAll("[^A-Za-z0-9._-]", "_");
        return "tts/%s/%d/%s/%s.%s".formatted(
                asset.getStory().getId(),
                asset.getStoryVersion(),
                safeVoice,
                asset.getId(),
                extension);
    }

    private String providerErrorMessage(TtsProviderError error) {
        if (error == null || error.message() == null || error.message().isBlank()) {
            return "TTS generation failed";
        }
        return error.message();
    }

    private ApiException providerFailure(RuntimeException ex) {
        if (ex instanceof ApiException apiException) {
            return apiException;
        }
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.TTS_GENERATION_FAILED, "TTS provider request failed");
    }

    private ApiException invalidProviderResponse(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_TTS_RESPONSE, message);
    }
}
