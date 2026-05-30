package com.lexease.recordings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.progress.ProgressService;
import com.lexease.progress.dtos.res.ProgressSummaryResponse;
import com.lexease.reading.ReadingSession;
import com.lexease.reading.ReadingSessionRepository;
import com.lexease.recordings.dtos.req.CreateRecordingRequest;
import com.lexease.recordings.dtos.req.VoicePayloadRequest;
import com.lexease.recordings.dtos.res.RecordingResponse;
import com.lexease.scoring.provider.ScoringEvaluationJobRequest;
import com.lexease.scoring.provider.ScoringEvaluationResult;
import com.lexease.scoring.provider.ScoringJobSubmitResponse;
import com.lexease.scoring.provider.ScoringProviderClient;
import com.lexease.shared.api.ApiException;
import com.lexease.stories.Story;
import com.lexease.stories.StoryRepository;
import com.lexease.stories.StoryStatus;
import com.lexease.stories.StoryTextProcessor;
import com.lexease.stories.StoryWord;
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
class RecordingServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private RecordingService recordingService;
    @Autowired
    private ProgressService progressService;
    @Autowired
    private ReadingSessionRepository sessionRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianChildLinkRepository guardianChildLinkRepository;
    @Autowired
    private AiEvaluationRepository evaluationRepository;

    @MockitoBean
    private ScoringProviderClient scoringProviderClient;

    @Test
    void childCreatesRecordingAndQueuesEvaluation() {
        UserAccount child = saveUser(UserRole.CHILD);
        ReadingSession session = saveSession(child, saveStory("Ngày xưa có mèo"));
        when(scoringProviderClient.submitJob(any(ScoringEvaluationJobRequest.class)))
                .thenReturn(new ScoringJobSubmitResponse("score-job-1", "QUEUED", null));

        RecordingResponse response = recordingService.create(
                child.getId(),
                UserRole.CHILD,
                session.getId(),
                createRequest());

        assertThat(response.wordCount()).isEqualTo(4);
        assertThat(response.expectedText()).isEqualTo("Ngày xưa");
        assertThat(response.mimeType()).isEqualTo("audio/webm");
        assertThat(response.audioUrl()).isNotNull();
        assertThat(response.evaluation().status()).isEqualTo(EvaluationStatus.PROCESSING);
        assertThat(response.evaluation().providerJobId()).isEqualTo("score-job-1");

        ArgumentCaptor<ScoringEvaluationJobRequest> requestCaptor = ArgumentCaptor.forClass(ScoringEvaluationJobRequest.class);
        verify(scoringProviderClient).submitJob(requestCaptor.capture());
        ScoringEvaluationJobRequest scoringRequest = requestCaptor.getValue();
        assertThat(scoringRequest.story().expectedText()).isEqualTo("Ngày xưa");
        assertThat(scoringRequest.audio().audioUrl()).isNotNull();
        assertThat(scoringRequest.audio().mimeType()).isEqualTo("audio/webm");
    }

    @Test
    void rejectsExpectedTextOutsideStory() {
        UserAccount child = saveUser(UserRole.CHILD);
        ReadingSession session = saveSession(child, saveStory("Ngày xưa có mèo"));

        assertThatThrownBy(() -> recordingService.create(
                child.getId(),
                UserRole.CHILD,
                session.getId(),
                createRequest("Một đoạn không thuộc truyện")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Expected text must belong to the session story");
    }

    @Test
    void scoringCallbackStoresDoneResult() {
        UserAccount child = saveUser(UserRole.CHILD);
        ReadingSession session = saveSession(child, saveStory("Ngày xưa có mèo"));
        when(scoringProviderClient.submitJob(any(ScoringEvaluationJobRequest.class)))
                .thenReturn(new ScoringJobSubmitResponse("score-job-2", "QUEUED", null));
        RecordingResponse created = recordingService.create(child.getId(), UserRole.CHILD, session.getId(), createRequest());

        recordingService.handleCallback(created.evaluation().id(), new ScoringEvaluationResult(
                created.evaluation().id().toString(),
                "DONE",
                "Ngày xưa",
                "Đọc rõ.",
                Map.of("accuracy", 0.9, "fluency", 0.8, "pace", 0.7),
                List.of(Map.of("wordIndex", 0, "expected", "Ngày", "heard", "Ngày", "correct", true)),
                List.of("xưa"),
                null));

        AiEvaluation evaluation = evaluationRepository.findByIdAndDeletedAtIsNull(created.evaluation().id()).orElseThrow();
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.DONE);
        assertThat(evaluation.getHeardText()).isEqualTo("Ngày xưa");
        assertThat(evaluation.getDifficultWords()).containsExactly("xưa");
    }

    @Test
    void guardianProgressSummarizesChildPractice() {
        UserAccount child = saveUser(UserRole.CHILD);
        UserAccount guardian = saveUser(UserRole.GUARDIAN);
        linkAccepted(guardian, child);
        ReadingSession session = saveSession(child, saveStory("Ngày xưa có mèo"));
        session.updateProgress(20, 60000, NOW);
        when(scoringProviderClient.submitJob(any(ScoringEvaluationJobRequest.class)))
                .thenReturn(new ScoringJobSubmitResponse("score-job-3", "QUEUED", null));
        RecordingResponse created = recordingService.create(child.getId(), UserRole.CHILD, session.getId(), createRequest());
        recordingService.handleCallback(created.evaluation().id(), new ScoringEvaluationResult(
                created.evaluation().id().toString(),
                "DONE",
                "Ngày xưa",
                "Đọc khá tốt.",
                Map.of("accuracy", 0.75, "fluency", 0.7, "pace", 0.8),
                List.of(Map.of("wordIndex", 1, "expected", "xưa", "heard", "xa", "correct", false, "errorType", "pronunciation")),
                List.of("xưa"),
                null));

        ProgressSummaryResponse summary = progressService.summary(guardian.getId(), UserRole.GUARDIAN, child.getId(), "month");

        assertThat(summary.sessionsCount()).isEqualTo(1);
        assertThat(summary.recordedSessionsCount()).isEqualTo(1);
        assertThat(summary.averageAccuracy()).isEqualTo(0.75);
        assertThat(summary.averageErrorsPerSession()).isEqualTo(1.0);
    }

    @Test
    void unrelatedGuardianCannotReadRecording() {
        UserAccount child = saveUser(UserRole.CHILD);
        UserAccount guardian = saveUser(UserRole.GUARDIAN);
        ReadingSession session = saveSession(child, saveStory("Ngày xưa có mèo"));
        when(scoringProviderClient.submitJob(any(ScoringEvaluationJobRequest.class)))
                .thenReturn(new ScoringJobSubmitResponse("score-job-4", "QUEUED", null));
        RecordingResponse created = recordingService.create(child.getId(), UserRole.CHILD, session.getId(), createRequest());

        assertThatThrownBy(() -> recordingService.get(guardian.getId(), UserRole.GUARDIAN, created.id()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot access recording");
    }

    private CreateRecordingRequest createRequest() {
        return createRequest("Ngày xưa");
    }

    private CreateRecordingRequest createRequest(String expectedText) {
        String audio = Base64.getEncoder().encodeToString("voice".getBytes(StandardCharsets.UTF_8));
        return new CreateRecordingRequest(
                1200L,
                expectedText,
                new VoicePayloadRequest("audio/webm", audio));
    }

    private ReadingSession saveSession(UserAccount child, Story story) {
        return sessionRepository.save(new ReadingSession(UUID.randomUUID(), child, story, "Binh", NOW));
    }

    private void linkAccepted(UserAccount guardian, UserAccount child) {
        GuardianChildLink link = new GuardianChildLink(
                UUID.randomUUID(),
                guardian,
                child,
                GuardianChildLinkStatus.PENDING,
                guardian,
                NOW);
        link.accept(NOW);
        guardianChildLinkRepository.save(link);
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

    private Story saveStory(String content) {
        UserAccount admin = saveUser(UserRole.ADMIN);
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
