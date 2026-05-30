package com.lexease.progress;

import com.lexease.guardians.PermissionService;
import com.lexease.progress.dtos.res.DifficultWordProgressResponse;
import com.lexease.progress.dtos.res.ProgressBucketResponse;
import com.lexease.progress.dtos.res.ProgressSessionDetailResponse;
import com.lexease.progress.dtos.res.ProgressSessionResponse;
import com.lexease.progress.dtos.res.ProgressSummaryResponse;
import com.lexease.progress.dtos.res.ProgressTrendResponse;
import com.lexease.reading.ReadingEventRepository;
import com.lexease.reading.ReadingEventType;
import com.lexease.reading.ReadingSession;
import com.lexease.reading.ReadingSessionRepository;
import com.lexease.reading.ReadingSessionStatus;
import com.lexease.recordings.AiEvaluation;
import com.lexease.recordings.AiEvaluationRepository;
import com.lexease.recordings.EvaluationStatus;
import com.lexease.recordings.Recording;
import com.lexease.recordings.RecordingRepository;
import com.lexease.recordings.RecordingService;
import com.lexease.recordings.dtos.res.RecordingResponse;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.users.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {
    private final PermissionService permissionService;
    private final ReadingSessionRepository sessionRepository;
    private final ReadingEventRepository eventRepository;
    private final RecordingRepository recordingRepository;
    private final AiEvaluationRepository evaluationRepository;
    private final RecordingService recordingService;
    private final Clock clock;

    public ProgressService(
            PermissionService permissionService,
            ReadingSessionRepository sessionRepository,
            ReadingEventRepository eventRepository,
            RecordingRepository recordingRepository,
            AiEvaluationRepository evaluationRepository,
            RecordingService recordingService,
            Clock clock
    ) {
        this.permissionService = permissionService;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.recordingRepository = recordingRepository;
        this.evaluationRepository = evaluationRepository;
        this.recordingService = recordingService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(UUID currentUserId, UserRole role, UUID childId, String range) {
        requireChildAccess(currentUserId, role, childId);
        TimeRange current = resolveRange(range, 0);
        TimeRange previous = resolveRange(range, 1);
        Stats currentStats = stats(childId, current);
        Stats previousStats = stats(childId, previous);
        return new ProgressSummaryResponse(
                childId,
                current.label(),
                currentStats.practiceMinutes(),
                currentStats.sessionsCount(),
                currentStats.completedSessionsCount(),
                currentStats.recordedSessionsCount(),
                round(currentStats.averageReadingSpeedWpm()),
                round(currentStats.averageAccuracy()),
                round(currentStats.averageFluency()),
                round(currentStats.averagePace()),
                round(currentStats.averageErrorsPerSession()),
                currentStats.ttsHelpCount(),
                new ProgressTrendResponse(
                        trend(currentStats.practiceMinutes(), previousStats.practiceMinutes()),
                        trend(currentStats.averageReadingSpeedWpm(), previousStats.averageReadingSpeedWpm()),
                        trend(currentStats.averageAccuracy(), previousStats.averageAccuracy()),
                        trend(currentStats.averageErrorsPerSession(), previousStats.averageErrorsPerSession())));
    }

    @Transactional(readOnly = true)
    public List<ProgressBucketResponse> timeseries(UUID currentUserId, UserRole role, UUID childId, String range) {
        requireChildAccess(currentUserId, role, childId);
        TimeRange timeRange = resolveRange(range, 0);
        Map<LocalDate, StatsAccumulator> buckets = new LinkedHashMap<>();
        LocalDate startDate = LocalDate.ofInstant(timeRange.start(), ZoneOffset.UTC);
        for (int i = 0; i < timeRange.days(); i++) {
            buckets.put(startDate.plusDays(i), new StatsAccumulator());
        }
        List<ReadingSession> sessions = sessionRepository
                .findByChildIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        childId,
                        timeRange.start(),
                        timeRange.end());
        sessions.forEach(session -> bucket(buckets, session.getStartedAt()).addSession(session));
        List<Recording> recordings = recordingRepository
                .findByChildIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        childId,
                        timeRange.start(),
                        timeRange.end());
        recordings.forEach(recording -> bucket(buckets, recording.getCreatedAt()).recordedSessions++);
        List<AiEvaluation> evaluations = evaluationRepository.findByChildAndCreatedAtRange(childId, timeRange.start(), timeRange.end());
        evaluations.forEach(evaluation -> bucket(buckets, evaluation.getCreatedAt()).addEvaluation(evaluation));
        for (LocalDate date : buckets.keySet()) {
            Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            buckets.get(date).ttsHelpCount = eventRepository.countByChildAndTypeAndCreatedAtRange(
                    childId,
                    ReadingEventType.TTS_HELP,
                    start,
                    end);
        }
        return buckets.entrySet().stream()
                .map(entry -> {
                    Stats stats = entry.getValue().toStats();
                    return new ProgressBucketResponse(
                            entry.getKey(),
                            stats.practiceMinutes(),
                            stats.sessionsCount(),
                            stats.recordedSessionsCount(),
                            round(stats.averageReadingSpeedWpm()),
                            round(stats.averageAccuracy()),
                            round(stats.averageErrorsPerSession()),
                            stats.ttsHelpCount());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DifficultWordProgressResponse> difficultWords(UUID currentUserId, UserRole role, UUID childId, String range) {
        requireChildAccess(currentUserId, role, childId);
        TimeRange timeRange = resolveRange(range, 0);
        Map<String, Long> counts = new HashMap<>();
        for (AiEvaluation evaluation : evaluationRepository.findByChildAndCreatedAtRange(childId, timeRange.start(), timeRange.end())) {
            if (evaluation.getStatus() != EvaluationStatus.DONE) {
                continue;
            }
            for (String word : evaluation.getDifficultWords()) {
                if (word != null && !word.isBlank()) {
                    counts.merge(word.trim().toLowerCase(), 1L, Long::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(30)
                .map(entry -> new DifficultWordProgressResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressSessionResponse> sessions(UUID currentUserId, UserRole role, UUID childId, String range) {
        requireChildAccess(currentUserId, role, childId);
        TimeRange timeRange = resolveRange(range, 0);
        return sessionRepository.findByChildIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        childId,
                        timeRange.start(),
                        timeRange.end()).stream()
                .map(session -> toSessionResponse(childId, session))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressSessionDetailResponse sessionDetail(UUID currentUserId, UserRole role, UUID childId, UUID sessionId) {
        requireChildAccess(currentUserId, role, childId);
        ReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.READING_SESSION_NOT_FOUND, "Reading session not found"));
        if (!session.getChild().getId().equals(childId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.READING_SESSION_NOT_FOUND, "Reading session not found");
        }
        List<RecordingResponse> recordings = recordingRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId).stream()
                .map(recording -> recordingService.get(currentUserId, role, recording.getId()))
                .toList();
        return new ProgressSessionDetailResponse(toSessionResponse(childId, session), recordings);
    }

    private ProgressSessionResponse toSessionResponse(UUID childId, ReadingSession session) {
        List<Recording> recordings = recordingRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtDesc(session.getId());
        AiEvaluation latestEvaluation = recordings.stream()
                .flatMap(recording -> evaluationRepository
                        .findFirstByRecordingIdAndDeletedAtIsNullOrderByCreatedAtDesc(recording.getId()).stream())
                .max(Comparator.comparing(AiEvaluation::getCreatedAt))
                .orElse(null);
        return new ProgressSessionResponse(
                session.getId(),
                session.getStory().getId(),
                session.getStory().getTitle(),
                session.getStatus(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getElapsedMs(),
                session.getCurrentWordIndex(),
                round(readingSpeedWpm(session)),
                recordings.size(),
                latestEvaluation == null ? null : latestEvaluation.getStatus(),
                latestEvaluation == null ? null : score(latestEvaluation, "accuracy"));
    }

    private Stats stats(UUID childId, TimeRange timeRange) {
        StatsAccumulator accumulator = new StatsAccumulator();
        sessionRepository.findByChildIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        childId,
                        timeRange.start(),
                        timeRange.end())
                .forEach(accumulator::addSession);
        recordingRepository.findByChildIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        childId,
                        timeRange.start(),
                        timeRange.end())
                .forEach(recording -> accumulator.recordedSessions++);
        evaluationRepository.findByChildAndCreatedAtRange(childId, timeRange.start(), timeRange.end())
                .forEach(accumulator::addEvaluation);
        accumulator.ttsHelpCount = eventRepository.countByChildAndTypeAndCreatedAtRange(
                childId,
                ReadingEventType.TTS_HELP,
                timeRange.start(),
                timeRange.end());
        return accumulator.toStats();
    }

    private StatsAccumulator bucket(Map<LocalDate, StatsAccumulator> buckets, Instant instant) {
        LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
        return buckets.computeIfAbsent(date, ignored -> new StatsAccumulator());
    }

    private TimeRange resolveRange(String range, int offsetPeriods) {
        String label = range == null || range.isBlank() ? "week" : range.trim().toLowerCase();
        int days = switch (label) {
            case "month" -> 30;
            case "week" -> 7;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Unsupported progress range");
        };
        Instant end = Instant.now(clock).minus(Duration.ofDays((long) days * offsetPeriods));
        Instant start = end.minus(Duration.ofDays(days));
        return new TimeRange(label, days, start, end);
    }

    private void requireChildAccess(UUID currentUserId, UserRole role, UUID childId) {
        if (!permissionService.canAccessChild(currentUserId, role, childId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Cannot access child progress");
        }
    }

    private double readingSpeedWpm(ReadingSession session) {
        if (session.getElapsedMs() <= 0 || session.getCurrentWordIndex() <= 0) {
            return 0;
        }
        double minutes = session.getElapsedMs() / 60000.0;
        return (session.getCurrentWordIndex() + 1) / minutes;
    }

    private double score(AiEvaluation evaluation, String key) {
        Object value = evaluation.getScores().get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0;
    }

    private long errorCount(AiEvaluation evaluation) {
        return evaluation.getWordResults().stream()
                .filter(word -> Boolean.FALSE.equals(word.get("correct")) || word.get("errorType") != null)
                .count();
    }

    private String trend(double current, double previous) {
        if (previous == 0 && current == 0) {
            return "0%";
        }
        if (previous == 0) {
            return "+100%";
        }
        double change = ((current - previous) / previous) * 100.0;
        return (change > 0 ? "+" : "") + Math.round(change) + "%";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record TimeRange(String label, int days, Instant start, Instant end) {
    }

    private record Stats(
            long practiceMinutes,
            int sessionsCount,
            int completedSessionsCount,
            int recordedSessionsCount,
            double averageReadingSpeedWpm,
            double averageAccuracy,
            double averageFluency,
            double averagePace,
            double averageErrorsPerSession,
            long ttsHelpCount
    ) {
    }

    private class StatsAccumulator {
        long elapsedMs;
        int sessionsCount;
        int completedSessionsCount;
        int recordedSessions;
        double readingSpeedSum;
        int readingSpeedCount;
        double accuracySum;
        int accuracyCount;
        double fluencySum;
        int fluencyCount;
        double paceSum;
        int paceCount;
        long errorCount;
        int evaluatedSessions;
        long ttsHelpCount;

        void addSession(ReadingSession session) {
            sessionsCount++;
            elapsedMs += session.getElapsedMs();
            if (session.getStatus() == ReadingSessionStatus.COMPLETED) {
                completedSessionsCount++;
            }
            double speed = readingSpeedWpm(session);
            if (speed > 0) {
                readingSpeedSum += speed;
                readingSpeedCount++;
            }
        }

        void addEvaluation(AiEvaluation evaluation) {
            if (evaluation.getStatus() != EvaluationStatus.DONE) {
                return;
            }
            accuracySum += score(evaluation, "accuracy");
            accuracyCount++;
            fluencySum += score(evaluation, "fluency");
            fluencyCount++;
            paceSum += score(evaluation, "pace");
            paceCount++;
            errorCount += errorCount(evaluation);
            evaluatedSessions++;
        }

        Stats toStats() {
            return new Stats(
                    Math.round(elapsedMs / 60000.0),
                    sessionsCount,
                    completedSessionsCount,
                    recordedSessions,
                    readingSpeedCount == 0 ? 0 : readingSpeedSum / readingSpeedCount,
                    accuracyCount == 0 ? 0 : accuracySum / accuracyCount,
                    fluencyCount == 0 ? 0 : fluencySum / fluencyCount,
                    paceCount == 0 ? 0 : paceSum / paceCount,
                    evaluatedSessions == 0 ? 0 : (double) errorCount / evaluatedSessions,
                    ttsHelpCount);
        }
    }
}
