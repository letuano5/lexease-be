package com.lexease.reading;

import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.storage.ObjectStorageService;
import com.lexease.stories.Story;
import com.lexease.stories.StoryAccessBlockRepository;
import com.lexease.stories.StoryRepository;
import com.lexease.stories.StoryStatus;
import com.lexease.tts.TtsAsset;
import com.lexease.tts.TtsAssetRepository;
import com.lexease.tts.TtsService;
import com.lexease.tts.TtsStatus;
import com.lexease.tts.TtsWordTimingRepository;
import com.lexease.tts.TtsProperties;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import com.lexease.reading.dtos.req.ReadingProgressEventRequest;
import com.lexease.reading.dtos.req.StartReadingSessionRequest;
import com.lexease.reading.dtos.req.UpdateReadingProgressRequest;
import com.lexease.reading.dtos.res.ReadingSessionResponse;
import com.lexease.reading.dtos.res.ReadingStoryResponse;
import com.lexease.reading.dtos.res.ReadingTtsResponse;
import com.lexease.reading.dtos.res.ResumePositionResponse;
import com.lexease.reading.dtos.res.WordTimingResponse;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingService {
    private final ReadingSessionRepository sessionRepository;
    private final ReadingEventRepository eventRepository;
    private final StoryRepository storyRepository;
    private final StoryAccessBlockRepository storyAccessBlockRepository;
    private final UserRepository userRepository;
    private final TtsService ttsService;
    private final TtsAssetRepository ttsAssetRepository;
    private final TtsWordTimingRepository ttsWordTimingRepository;
    private final ObjectStorageService objectStorageService;
    private final TtsProperties ttsProperties;
    private final Clock clock;

    public ReadingService(
            ReadingSessionRepository sessionRepository,
            ReadingEventRepository eventRepository,
            StoryRepository storyRepository,
            StoryAccessBlockRepository storyAccessBlockRepository,
            UserRepository userRepository,
            TtsService ttsService,
            TtsAssetRepository ttsAssetRepository,
            TtsWordTimingRepository ttsWordTimingRepository,
            ObjectStorageService objectStorageService,
            TtsProperties ttsProperties,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.storyRepository = storyRepository;
        this.storyAccessBlockRepository = storyAccessBlockRepository;
        this.userRepository = userRepository;
        this.ttsService = ttsService;
        this.ttsAssetRepository = ttsAssetRepository;
        this.ttsWordTimingRepository = ttsWordTimingRepository;
        this.objectStorageService = objectStorageService;
        this.ttsProperties = ttsProperties;
        this.clock = clock;
    }

    @Transactional
    public ReadingSessionResponse start(UUID currentUserId, UserRole role, StartReadingSessionRequest request) {
        UserAccount child = requireChild(currentUserId, role);
        Story story = requireReadableStory(child.getId(), request.storyId());
        String voice = resolveVoice(request.voice());
        TtsAsset asset = ttsService.ensureAssetQueued(story.getId(), voice);
        Instant now = Instant.now(clock);
        ReadingSession session;
        if (request.resolvedMode() == ReadingSessionMode.RESUME_OR_START) {
            session = sessionRepository
                    .findFirstByChildIdAndStoryIdAndVoiceIdAndStatusOrderByLastActiveAtDesc(
                            child.getId(),
                            story.getId(),
                            voice,
                            ReadingSessionStatus.IN_PROGRESS)
                    .orElseGet(() -> createSession(child, story, voice, now));
            session.touch(now);
        } else {
            session = createSession(child, story, voice, now);
        }
        eventRepository.save(new ReadingEvent(
                UUID.randomUUID(),
                session,
                ReadingEventType.START,
                null,
                null,
                null,
                Map.of(),
                now));
        return toResponse(session, asset);
    }

    @Transactional(readOnly = true)
    public ReadingSessionResponse active(UUID currentUserId, UserRole role, UUID storyId, String requestedVoice) {
        UserAccount child = requireChild(currentUserId, role);
        String voice = resolveVoice(requestedVoice);
        ReadingSession session = sessionRepository
                .findFirstByChildIdAndStoryIdAndVoiceIdAndStatusOrderByLastActiveAtDesc(
                        child.getId(),
                        storyId,
                        voice,
                        ReadingSessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.READING_SESSION_NOT_FOUND, "Reading session not found"));
        requireReadableStory(child.getId(), session.getStory().getId());
        return toResponse(session, findAsset(session));
    }

    @Transactional(readOnly = true)
    public ReadingSessionResponse get(UUID currentUserId, UserRole role, UUID sessionId) {
        UserAccount child = requireChild(currentUserId, role);
        ReadingSession session = findOwnedSession(sessionId, child.getId());
        requireReadableStory(child.getId(), session.getStory().getId());
        return toResponse(session, findAsset(session));
    }

    @Transactional
    public ReadingSessionResponse updateProgress(
            UUID currentUserId,
            UserRole role,
            UUID sessionId,
            UpdateReadingProgressRequest request
    ) {
        UserAccount child = requireChild(currentUserId, role);
        ReadingSession session = findOwnedSession(sessionId, child.getId());
        if (session.getStatus() != ReadingSessionStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_READING_PROGRESS, "Reading session is not in progress");
        }
        Instant now = Instant.now(clock);
        session.updateProgress(request.currentWordIndex(), request.elapsedMs(), now);
        saveEvents(session, request.resolvedEvents(), now);
        return toResponse(session, findAsset(session));
    }

    @Transactional
    public ReadingSessionResponse complete(UUID currentUserId, UserRole role, UUID sessionId) {
        UserAccount child = requireChild(currentUserId, role);
        ReadingSession session = findOwnedSession(sessionId, child.getId());
        Instant now = Instant.now(clock);
        session.complete(now);
        eventRepository.save(new ReadingEvent(
                UUID.randomUUID(),
                session,
                ReadingEventType.COMPLETE,
                null,
                session.getCurrentWordIndex(),
                session.getElapsedMs(),
                Map.of(),
                now));
        return toResponse(session, findAsset(session));
    }

    private ReadingSession createSession(UserAccount child, Story story, String voice, Instant now) {
        return sessionRepository.save(new ReadingSession(UUID.randomUUID(), child, story, voice, now));
    }

    private void saveEvents(ReadingSession session, List<ReadingProgressEventRequest> events, Instant now) {
        List<ReadingEvent> entities = events.stream()
                .map(event -> new ReadingEvent(
                        UUID.randomUUID(),
                        session,
                        event.type(),
                        event.word(),
                        event.wordIndex(),
                        event.timestampMs(),
                        event.metadata(),
                        now))
                .toList();
        eventRepository.saveAll(entities);
    }

    private ReadingSessionResponse toResponse(ReadingSession session, TtsAsset asset) {
        ReadingTtsResponse tts = toTtsResponse(asset);
        return new ReadingSessionResponse(
                session.getId(),
                session.getStatus(),
                ReadingStoryResponse.from(session.getStory()),
                tts,
                new ResumePositionResponse(session.getCurrentWordIndex()),
                session.getElapsedMs());
    }

    private ReadingTtsResponse toTtsResponse(TtsAsset asset) {
        if (asset == null) {
            return new ReadingTtsResponse(TtsStatus.PENDING, null, ttsProperties.defaultVoice(), null, List.of());
        }
        URI audioUrl = asset.getStatus() == TtsStatus.READY && asset.getAudioObjectKey() != null
                ? objectStorageService.getSignedReadUrl(asset.getAudioObjectKey())
                : null;
        List<WordTimingResponse> timings = asset.getStatus() == TtsStatus.READY
                ? ttsWordTimingRepository.findByAssetIdOrderByWordIndex(asset.getId()).stream()
                .map(WordTimingResponse::from)
                .toList()
                : List.of();
        return new ReadingTtsResponse(asset.getStatus(), asset.getId(), asset.getVoiceId(), audioUrl, timings);
    }

    private TtsAsset findAsset(ReadingSession session) {
        return ttsAssetRepository
                .findByStoryIdAndStoryVersionAndVoiceId(
                        session.getStory().getId(),
                        session.getStoryVersion(),
                        session.getVoiceId())
                .orElse(null);
    }

    private UserAccount requireChild(UUID currentUserId, UserRole role) {
        if (role != UserRole.CHILD) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.READING_SESSION_FORBIDDEN, "Only children can use reading sessions");
        }
        UserAccount child = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, "Child not found"));
        if (child.getRole() != UserRole.CHILD || child.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.READING_SESSION_FORBIDDEN, "Invalid child");
        }
        return child;
    }

    private Story requireReadableStory(UUID childId, UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found"));
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found");
        }
        if (storyAccessBlockRepository.existsAcceptedActiveBlock(
                childId,
                story.getId(),
                GuardianChildLinkStatus.ACCEPTED)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found");
        }
        return story;
    }

    private ReadingSession findOwnedSession(UUID sessionId, UUID childId) {
        ReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.READING_SESSION_NOT_FOUND, "Reading session not found"));
        if (!session.getChild().getId().equals(childId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.READING_SESSION_FORBIDDEN, "Cannot access reading session");
        }
        return session;
    }

    private String resolveVoice(String requestedVoice) {
        return requestedVoice == null || requestedVoice.isBlank() ? ttsProperties.defaultVoice() : requestedVoice.trim();
    }
}
