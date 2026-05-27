package com.lexease.tts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexease.shared.api.ApiException;
import com.lexease.stories.Story;
import com.lexease.stories.StoryRepository;
import com.lexease.stories.StoryStatus;
import com.lexease.stories.StoryTextProcessor;
import com.lexease.stories.StoryWord;
import com.lexease.tts.dtos.req.TtsCallbackRequest;
import com.lexease.tts.dtos.res.TtsAssetGenerationResult;
import com.lexease.tts.provider.TtsAudio;
import com.lexease.tts.provider.TtsJobSubmitRequest;
import com.lexease.tts.provider.TtsJobSubmitResponse;
import com.lexease.tts.provider.TtsProviderClient;
import com.lexease.tts.provider.TtsProviderError;
import com.lexease.tts.provider.TtsProviderWord;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TtsServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private TtsService ttsService;
    @Autowired
    private TtsAssetRepository ttsAssetRepository;
    @Autowired
    private TtsWordTimingRepository timingRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TtsProviderClient providerClient;

    @Test
    void callbackRequestAllowsMetadataAndFutureFields() throws Exception {
        String body = """
                {
                  "jobId": "job-001",
                  "status": "READY",
                  "requestId": "asset-001",
                  "voice": "Binh",
                  "language": "vi-VN",
                  "audio": {
                    "mimeType": "audio/wav",
                    "format": "wav",
                    "sampleRateHz": 24000,
                    "durationMs": 1000,
                    "contentBase64": "YXVkaW8="
                  },
                  "words": [
                    {
                      "index": 0,
                      "text": "Ngay",
                      "normalizedText": "ngay",
                      "startMs": 0,
                      "endMs": 300
                    }
                  ],
                  "metadata": {
                    "model": "vieneu-tts",
                    "alignmentMethod": "mfa",
                    "createdAt": "2026-05-27T16:50:28.246562Z"
                  },
                  "futureField": "ignored"
                }
                """;

        TtsCallbackRequest request = objectMapper.readValue(body, TtsCallbackRequest.class);

        assertThat(request.metadata()).containsEntry("model", "vieneu-tts");
        assertThat(request.words()).hasSize(1);
    }

    @Test
    void asyncSubmitStoresProviderJobId() {
        Story story = saveStory("Ngày xưa");
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-001", null, "QUEUED"));

        TtsAssetGenerationResult result = ttsService.generateAsset(story.getId(), "Binh", false);

        assertThat(result.accepted()).isTrue();
        TtsAsset asset = ttsAssetRepository.findById(result.response().assetId()).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(TtsStatus.PROCESSING);
        assertThat(asset.getProviderJobId()).isEqualTo("job-001");
        assertThat(asset.getProviderRequestId()).isEqualTo(asset.getId().toString());

        ArgumentCaptor<TtsJobSubmitRequest> requestCaptor = ArgumentCaptor.forClass(TtsJobSubmitRequest.class);
        verify(providerClient).submitJob(requestCaptor.capture());
        TtsJobSubmitRequest submittedRequest = requestCaptor.getValue();
        assertThat(submittedRequest.requestId()).isEqualTo(asset.getId().toString());
        assertThat(submittedRequest.callbackUrl())
                .isEqualTo("http://localhost:8080/internal/tts/jobs/" + asset.getId() + "/callback");
        assertThat(submittedRequest.text()).isEqualTo(story.getContent());
        assertThat(submittedRequest.voice()).isEqualTo("Binh");
        assertThat(submittedRequest.audioFormat()).isEqualTo("wav");
        assertThat(submittedRequest.language()).isEqualTo("vi-VN");
    }

    @Test
    void successCallbackStoresAudioAndWordTimings() {
        Story story = saveStory("Ngày xưa");
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-002", null, "QUEUED"));
        UUID assetId = ttsService.generateAsset(story.getId(), "Binh", false).response().assetId();

        ttsService.handleCallback(assetId, new TtsCallbackRequest(
                "job-002",
                "READY",
                assetId.toString(),
                "Binh",
                "vi-VN",
                new TtsAudio("audio/wav", "wav", 24000, 1000, Base64.getEncoder().encodeToString("audio".getBytes(StandardCharsets.UTF_8))),
                List.of(
                        new TtsProviderWord(0, "Ngày", "ngay", 0, 300),
                        new TtsProviderWord(1, "xưa", "xua", 300, 600)),
                null,
                Map.of("model", "vieneu-tts")));

        TtsAsset asset = ttsAssetRepository.findById(assetId).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(TtsStatus.READY);
        assertThat(asset.getAudioObjectKey()).isNotBlank();
        assertThat(timingRepository.findByAssetIdOrderByWordIndex(assetId))
                .extracting(TtsWordTiming::getText)
                .containsExactly("Ngày", "xưa");
    }

    @Test
    void failedCallbackMarksAssetFailed() {
        Story story = saveStory("Ngày xưa");
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-003", null, "QUEUED"));
        UUID assetId = ttsService.generateAsset(story.getId(), "Binh", false).response().assetId();

        ttsService.handleCallback(assetId, new TtsCallbackRequest(
                "job-003",
                "FAILED",
                assetId.toString(),
                null,
                null,
                null,
                null,
                new TtsProviderError("TTS_GENERATION_FAILED", "Could not synthesize audio", true, Map.of()),
                null));

        TtsAsset asset = ttsAssetRepository.findById(assetId).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(TtsStatus.FAILED);
        assertThat(asset.getErrorMessage()).isEqualTo("Could not synthesize audio");
    }

    @Test
    void invalidTimingMarksAssetFailedAndRejectsCallback() {
        Story story = saveStory("Ngày xưa");
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-004", null, "QUEUED"));
        UUID assetId = ttsService.generateAsset(story.getId(), "Binh", false).response().assetId();

        assertThatThrownBy(() -> ttsService.handleCallback(assetId, new TtsCallbackRequest(
                "job-004",
                "READY",
                assetId.toString(),
                "Binh",
                "vi-VN",
                new TtsAudio("audio/wav", "wav", 24000, 1000, Base64.getEncoder().encodeToString("audio".getBytes(StandardCharsets.UTF_8))),
                List.of(new TtsProviderWord(0, "Ngày", "ngay", 400, 300)),
                null,
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("TTS word timings are invalid");
        assertThat(ttsAssetRepository.findById(assetId).orElseThrow().getStatus()).isEqualTo(TtsStatus.FAILED);
    }

    private Story saveStory(String content) {
        UserAccount admin = userRepository.save(new UserAccount(
                UUID.randomUUID(),
                UUID.randomUUID() + "@example.com",
                "hash",
                "Admin",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW));
        StoryTextProcessor processor = new StoryTextProcessor();
        Story story = new Story(
                UUID.randomUUID(),
                "Truyện",
                processor.normalizeForSearch("Truyện"),
                content,
                StoryStatus.PUBLISHED,
                admin,
                NOW,
                NOW);
        story.replaceWords(processor.splitWords(content).stream()
                .map(word -> new StoryWord(
                        UUID.randomUUID(),
                        story,
                        word.wordIndex(),
                        word.text(),
                        word.normalizedText(),
                        word.startChar(),
                        word.endChar()))
                .toList());
        return storyRepository.save(story);
    }
}
