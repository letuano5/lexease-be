package com.lexease.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.reading.dtos.req.ReadingProgressEventRequest;
import com.lexease.reading.dtos.req.StartReadingSessionRequest;
import com.lexease.reading.dtos.req.UpdateReadingProgressRequest;
import com.lexease.reading.dtos.res.ReadingSessionResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.stories.Story;
import com.lexease.stories.StoryAccessBlock;
import com.lexease.stories.StoryAccessBlockRepository;
import com.lexease.stories.StoryRepository;
import com.lexease.stories.StoryStatus;
import com.lexease.stories.StoryTextProcessor;
import com.lexease.stories.StoryWord;
import com.lexease.tts.TtsService;
import com.lexease.tts.dtos.req.TtsCallbackRequest;
import com.lexease.tts.provider.TtsAudio;
import com.lexease.tts.provider.TtsJobSubmitRequest;
import com.lexease.tts.provider.TtsJobSubmitResponse;
import com.lexease.tts.provider.TtsProviderClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReadingServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private ReadingService readingService;
    @Autowired
    private TtsService ttsService;
    @Autowired
    private ReadingSessionRepository sessionRepository;
    @Autowired
    private ReadingEventRepository eventRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianChildLinkRepository guardianChildLinkRepository;
    @Autowired
    private StoryAccessBlockRepository storyAccessBlockRepository;

    @MockitoBean
    private TtsProviderClient providerClient;

    @Test
    void childStartsAndResumesActiveSession() {
        UserAccount child = saveUser(UserRole.CHILD);
        Story story = saveStory("Ngày xưa", StoryStatus.PUBLISHED);
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-reading-001", null, "QUEUED"));

        ReadingSessionResponse first = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START));
        ReadingSessionResponse resumed = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START));

        assertThat(resumed.sessionId()).isEqualTo(first.sessionId());
        assertThat(sessionRepository.count()).isEqualTo(1);
    }

    @Test
    void startFromBeginningCreatesNewSession() {
        UserAccount child = saveUser(UserRole.CHILD);
        Story story = saveStory("Ngày xưa", StoryStatus.PUBLISHED);
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-reading-002", null, "QUEUED"));

        ReadingSessionResponse first = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START));
        ReadingSessionResponse second = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.START_FROM_BEGINNING));

        assertThat(second.sessionId()).isNotEqualTo(first.sessionId());
        assertThat(sessionRepository.count()).isEqualTo(2);
    }

    @Test
    void blockedStoryCannotStartReading() {
        UserAccount child = saveUser(UserRole.CHILD);
        UserAccount guardian = saveUser(UserRole.GUARDIAN);
        Story story = saveStory("Ngày xưa", StoryStatus.PUBLISHED);
        GuardianChildLink link = new GuardianChildLink(
                UUID.randomUUID(),
                guardian,
                child,
                GuardianChildLinkStatus.PENDING,
                guardian,
                NOW);
        link.accept(NOW);
        guardianChildLinkRepository.save(link);
        storyAccessBlockRepository.save(new StoryAccessBlock(
                UUID.randomUUID(),
                child,
                story,
                guardian,
                "blocked",
                NOW,
                NOW));

        assertThatThrownBy(() -> readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Story not found");
    }

    @Test
    void progressUpdatesCheckpointAndStoresEvents() {
        UserAccount child = saveUser(UserRole.CHILD);
        Story story = saveStory("Ngày xưa", StoryStatus.PUBLISHED);
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-reading-003", null, "QUEUED"));
        ReadingSessionResponse session = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START));

        ReadingSessionResponse updated = readingService.updateProgress(
                child.getId(),
                UserRole.CHILD,
                session.sessionId(),
                new UpdateReadingProgressRequest(
                        12,
                        94000L,
                        List.of(new ReadingProgressEventRequest(
                                ReadingEventType.TTS_HELP,
                                "kho",
                                12,
                                93000L,
                                Map.of()))));

        assertThat(updated.resumePosition().wordIndex()).isEqualTo(12);
        assertThat(updated.elapsedMs()).isEqualTo(94000L);
        assertThat(eventRepository.countBySessionId(session.sessionId())).isEqualTo(2);
    }

    @Test
    void readyTtsReturnsAudioUrlAndTimings() {
        UserAccount child = saveUser(UserRole.CHILD);
        Story story = saveStory("Ngày xưa", StoryStatus.PUBLISHED);
        when(providerClient.submitJob(any(TtsJobSubmitRequest.class)))
                .thenReturn(new TtsJobSubmitResponse("job-reading-004", null, "QUEUED"));
        UUID assetId = ttsService.generateAsset(story.getId(), "Binh", false).response().assetId();
        ttsService.handleCallback(assetId, new TtsCallbackRequest(
                "job-reading-004",
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

        ReadingSessionResponse response = readingService.start(child.getId(), UserRole.CHILD, new StartReadingSessionRequest(
                story.getId(),
                "Binh",
                ReadingSessionMode.RESUME_OR_START));

        assertThat(response.tts().audioUrl()).isNotNull();
        assertThat(response.tts().wordTimings()).hasSize(2);
    }

    private UserAccount saveUser(UserRole role) {
        return userRepository.save(new UserAccount(
                UUID.randomUUID(),
                UUID.randomUUID() + "@example.com",
                "hash",
                role.name(),
                role,
                UserStatus.ACTIVE,
                NOW,
                NOW));
    }

    private Story saveStory(String content, StoryStatus status) {
        UserAccount admin = saveUser(UserRole.ADMIN);
        StoryTextProcessor processor = new StoryTextProcessor();
        Story story = new Story(
                UUID.randomUUID(),
                "Truyện",
                processor.normalizeForSearch("Truyện"),
                content,
                status,
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
