package com.lexease.stories;

import com.lexease.authors.Author;
import com.lexease.authors.AuthorRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.guardians.PermissionService;
import com.lexease.genres.Genre;
import com.lexease.genres.GenreRepository;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import com.lexease.shared.api.PageResponse;
import com.lexease.shared.audit.AuditAction;
import com.lexease.shared.audit.AuditService;
import com.lexease.shared.audit.AuditTargetType;
import com.lexease.stories.dtos.req.PatchStoryRequest;
import com.lexease.stories.dtos.req.StoryAccessChangeRequest;
import com.lexease.stories.dtos.req.StoryUpsertRequest;
import com.lexease.stories.dtos.res.StoryAccessResponse;
import com.lexease.stories.dtos.res.StoryDetailResponse;
import com.lexease.stories.dtos.res.StorySummaryResponse;
import com.lexease.tts.TtsService;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final StoryRepository storyRepository;
    private final StoryAccessBlockRepository storyAccessBlockRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final StoryTextProcessor storyTextProcessor;
    private final AuditService auditService;
    private final TtsService ttsService;
    private final Clock clock;

    public StoryService(
            StoryRepository storyRepository,
            StoryAccessBlockRepository storyAccessBlockRepository,
            GenreRepository genreRepository,
            AuthorRepository authorRepository,
            UserRepository userRepository,
            PermissionService permissionService,
            StoryTextProcessor storyTextProcessor,
            AuditService auditService,
            TtsService ttsService,
            Clock clock
    ) {
        this.storyRepository = storyRepository;
        this.storyAccessBlockRepository = storyAccessBlockRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.storyTextProcessor = storyTextProcessor;
        this.auditService = auditService;
        this.ttsService = ttsService;
        this.clock = clock;
    }

    @Transactional
    public StoryDetailResponse create(UUID adminId, StoryUpsertRequest request) {
        UserAccount admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "User not found"));
        Instant now = Instant.now(clock);
        String title = request.title().trim();
        String content = request.content().trim();
        Story story = new Story(
                UUID.randomUUID(),
                title,
                storyTextProcessor.normalizeForSearch(title),
                content,
                request.status(),
                admin,
                now,
                now);
        replaceMetadataAndText(story, request, content);
        story = storyRepository.save(story);
        ttsService.enqueueDefaultAssetForPublishedStory(story);
        auditService.log(adminId, AuditAction.STORY_CREATED, AuditTargetType.STORY, story.getId());
        return StoryDetailResponse.from(story);
    }

    @Transactional
    public StoryDetailResponse update(UUID adminId, UUID storyId, PatchStoryRequest request) {
        Story story = findStory(storyId);
        String title = request.title() == null ? story.getTitle() : request.title().trim();
        String content = request.content() == null ? story.getContent() : request.content().trim();
        StoryStatus status = request.status() == null ? story.getStatus() : request.status();
        story.update(
                title,
                storyTextProcessor.normalizeForSearch(title),
                content,
                status,
                Instant.now(clock));
        if (request.genreIds() != null) {
            story.replaceGenres(loadGenres(request.genreIds()));
        }
        if (request.authorIds() != null) {
            story.replaceAuthors(loadAuthors(request.authorIds()));
        }
        if (request.content() != null) {
            replaceWords(story, content);
        }
        auditService.log(adminId, AuditAction.STORY_UPDATED, AuditTargetType.STORY, story.getId());
        Story saved = storyRepository.save(story);
        ttsService.enqueueDefaultAssetForPublishedStory(saved);
        return StoryDetailResponse.from(saved);
    }

    @Transactional
    public void archive(UUID adminId, UUID storyId) {
        Story story = findStory(storyId);
        story.archive(Instant.now(clock));
        storyRepository.save(story);
        auditService.log(adminId, AuditAction.STORY_ARCHIVED, AuditTargetType.STORY, story.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<StorySummaryResponse> search(
            UUID currentUserId,
            UserRole currentRole,
            String keyword,
            List<UUID> genreIds,
            List<UUID> authorIds,
            UUID childId,
            int page,
            int size
    ) {
        UUID effectiveChildId = resolveChildContext(currentUserId, currentRole, childId);
        boolean publishedOnly = currentRole != UserRole.ADMIN;
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        UUID blockedVisibilityChildId = currentRole == UserRole.CHILD ? effectiveChildId : null;
        Page<Story> storyPage = storyRepository.findAll(StorySpecifications.search(
                keyword == null ? null : storyTextProcessor.normalizeForSearch(keyword),
                distinctIds(genreIds),
                distinctIds(authorIds),
                publishedOnly,
                blockedVisibilityChildId), pageRequest);
        return PageResponse.from(storyPage.map(story -> StorySummaryResponse.from(
                story,
                effectiveChildId != null && storyAccessBlockRepository.existsAcceptedActiveBlock(
                        effectiveChildId,
                        story.getId(),
                        GuardianChildLinkStatus.ACCEPTED))));
    }

    @Transactional(readOnly = true)
    public StoryDetailResponse get(UUID currentUserId, UserRole currentRole, UUID storyId, UUID childId) {
        Story story = findStory(storyId);
        UUID effectiveChildId = resolveChildContext(currentUserId, currentRole, childId);
        if (currentRole != UserRole.ADMIN && story.getStatus() != StoryStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found");
        }
        if (currentRole == UserRole.CHILD
                && effectiveChildId != null
                && storyAccessBlockRepository.existsAcceptedActiveBlock(
                effectiveChildId,
                story.getId(),
                GuardianChildLinkStatus.ACCEPTED)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found");
        }
        return StoryDetailResponse.from(story);
    }

    @Transactional
    public StoryAccessResponse block(UUID currentUserId, UserRole currentRole, StoryAccessChangeRequest request) {
        if (request.blocked() != null && !request.blocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STORY_ACCESS_BLOCK, "Blocked must be true");
        }
        if (currentRole != UserRole.GUARDIAN) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.STORY_BLOCK_FORBIDDEN, "Only guardians can block stories");
        }
        if (!permissionService.canManageChild(currentUserId, currentRole, request.childId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.STORY_BLOCK_FORBIDDEN, "Cannot block story for child");
        }
        UserAccount child = findChild(request.childId());
        Story story = findStory(request.storyId());
        UserAccount guardian = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "User not found"));
        Instant now = Instant.now(clock);
        StoryAccessBlock block = storyAccessBlockRepository
                .findByBlockedByGuardianIdAndChildIdAndStoryIdAndActiveTrue(currentUserId, child.getId(), story.getId())
                .orElseGet(() -> new StoryAccessBlock(
                        UUID.randomUUID(),
                        child,
                        story,
                        guardian,
                        trimToNull(request.reason()),
                        now,
                        now));
        block.updateReason(trimToNull(request.reason()), now);
        storyAccessBlockRepository.save(block);
        auditService.log(currentUserId, AuditAction.STORY_BLOCKED, AuditTargetType.STORY_ACCESS_BLOCK, block.getId());
        return new StoryAccessResponse(child.getId(), story.getId(), true);
    }

    @Transactional
    public StoryAccessResponse unblock(UUID currentUserId, UserRole currentRole, StoryAccessChangeRequest request) {
        if (request.blocked() != null && request.blocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STORY_ACCESS_BLOCK, "Blocked must be false");
        }
        if (!permissionService.canManageChild(currentUserId, currentRole, request.childId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.STORY_BLOCK_FORBIDDEN, "Cannot unblock story for child");
        }
        List<StoryAccessBlock> blocks = findBlocksToUnblock(currentUserId, currentRole, request);
        Instant now = Instant.now(clock);
        for (StoryAccessBlock block : blocks) {
            block.deactivate(now);
            auditService.log(currentUserId, AuditAction.STORY_UNBLOCKED, AuditTargetType.STORY_ACCESS_BLOCK, block.getId());
        }
        storyAccessBlockRepository.saveAll(blocks);
        return new StoryAccessResponse(request.childId(), request.storyId(), false);
    }

    private List<StoryAccessBlock> findBlocksToUnblock(
            UUID currentUserId,
            UserRole currentRole,
            StoryAccessChangeRequest request
    ) {
        if (currentRole == UserRole.ADMIN) {
            List<StoryAccessBlock> blocks = storyAccessBlockRepository.findByChildIdAndStoryIdAndActiveTrue(
                    request.childId(),
                    request.storyId());
            if (blocks.isEmpty()) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.STORY_ACCESS_BLOCK_NOT_FOUND,
                        "Story block not found");
            }
            return blocks;
        }
        return List.of(storyAccessBlockRepository
                .findByBlockedByGuardianIdAndChildIdAndStoryIdAndActiveTrue(
                        currentUserId,
                        request.childId(),
                        request.storyId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.STORY_ACCESS_BLOCK_NOT_FOUND,
                        "Story block not found")));
    }

    private void replaceMetadataAndText(Story story, StoryUpsertRequest request, String content) {
        story.replaceGenres(loadGenres(request.genreIds()));
        story.replaceAuthors(loadAuthors(request.authorIds()));
        replaceWords(story, content);
    }

    private void replaceWords(Story story, String content) {
        story.replaceWords(storyTextProcessor.splitWords(content).stream()
                .map(word -> new StoryWord(
                        UUID.randomUUID(),
                        story,
                        word.wordIndex(),
                        word.text(),
                        word.normalizedText(),
                        word.startChar(),
                        word.endChar()))
                .toList());
    }

    private List<Genre> loadGenres(List<UUID> genreIds) {
        List<UUID> ids = distinctIds(genreIds);
        List<Genre> genres = genreRepository.findAllByIdInAndDeletedAtIsNull(ids);
        if (genres.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.GENRE_NOT_FOUND, "Genre not found");
        }
        return genres;
    }

    private List<Author> loadAuthors(List<UUID> authorIds) {
        List<UUID> ids = distinctIds(authorIds);
        List<Author> authors = authorRepository.findAllByIdInAndDeletedAtIsNull(ids);
        if (authors.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.AUTHOR_NOT_FOUND, "Author not found");
        }
        return authors;
    }

    private List<UUID> distinctIds(List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .distinct()
                .toList();
    }

    private Story findStory(UUID storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.STORY_NOT_FOUND, "Story not found"));
    }

    private UserAccount findChild(UUID childId) {
        UserAccount child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, "Child not found"));
        if (child.getRole() != UserRole.CHILD || child.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_CHILD, "Invalid child");
        }
        return child;
    }

    private UUID resolveChildContext(UUID currentUserId, UserRole currentRole, UUID childId) {
        if (currentRole == UserRole.CHILD) {
            if (childId != null && !currentUserId.equals(childId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Cannot access another child context");
            }
            return currentUserId;
        }
        if (childId == null) {
            return null;
        }
        if (!permissionService.canAccessChild(currentUserId, currentRole, childId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Cannot access child context");
        }
        return childId;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
