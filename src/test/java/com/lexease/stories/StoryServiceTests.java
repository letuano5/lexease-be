package com.lexease.stories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.PageResponse;
import com.lexease.stories.dtos.req.StoryAccessChangeRequest;
import com.lexease.stories.dtos.req.StoryUpsertRequest;
import com.lexease.stories.dtos.res.StoryDetailResponse;
import com.lexease.stories.dtos.res.StorySummaryResponse;
import com.lexease.users.UserAccount;
import com.lexease.users.UserRepository;
import com.lexease.users.UserRole;
import com.lexease.users.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoryServiceTests {
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    @Autowired
    private StoryService storyService;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianChildLinkRepository guardianChildLinkRepository;

    @Test
    void searchFiltersByGenreAndAuthorArrays() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child@example.com");
        Genre children = saveGenre("Thiếu nhi");
        Genre practice = saveGenre("Luyện đọc");
        Genre adventure = saveGenre("Phiêu lưu");
        Author nguyen = saveAuthor("Nguyễn A");
        Author tran = saveAuthor("Trần B");

        StoryDetailResponse catStory = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Chú mèo đi học",
                "Chú mèo đi học mỗi ngày.",
                List.of(children.getId(), practice.getId()),
                List.of(nguyen.getId(), tran.getId()),
                StoryStatus.PUBLISHED));
        storyService.create(admin.getId(), new StoryUpsertRequest(
                "Con thuyền nhỏ",
                "Một câu chuyện khác.",
                List.of(adventure.getId()),
                List.of(nguyen.getId()),
                StoryStatus.PUBLISHED));

        PageResponse<StorySummaryResponse> response = storyService.search(
                child.getId(),
                UserRole.CHILD,
                "meo",
                List.of(children.getId()),
                List.of(nguyen.getId()),
                child.getId(),
                0,
                20);

        assertThat(response.items())
                .extracting(StorySummaryResponse::id)
                .containsExactly(catStory.id());
    }

    @Test
    void childSearchExcludesStoryBlockedByAcceptedGuardian() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin2@example.com");
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child2@example.com");
        Genre genre = saveGenre("Thiếu nhi");
        Author author = saveAuthor("Nguyễn A");
        saveAcceptedLink(guardian, child);
        StoryDetailResponse story = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện bị chặn",
                "Nội dung truyện.",
                List.of(genre.getId()),
                List.of(author.getId()),
                StoryStatus.PUBLISHED));

        storyService.block(guardian.getId(), UserRole.GUARDIAN, new StoryAccessChangeRequest(
                child.getId(),
                story.id(),
                true,
                "Không phù hợp"));

        PageResponse<StorySummaryResponse> response = storyService.search(
                child.getId(),
                UserRole.CHILD,
                null,
                List.of(),
                List.of(),
                child.getId(),
                0,
                20);

        assertThat(response.items()).isEmpty();
    }

    @Test
    void guardianCannotBlockUnacceptedChild() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin3@example.com");
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian2@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child3@example.com");
        Genre genre = saveGenre("Thiếu nhi");
        Author author = saveAuthor("Nguyễn A");
        StoryDetailResponse story = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện",
                "Nội dung truyện.",
                List.of(genre.getId()),
                List.of(author.getId()),
                StoryStatus.PUBLISHED));

        assertThatThrownBy(() -> storyService.block(guardian.getId(), UserRole.GUARDIAN, new StoryAccessChangeRequest(
                child.getId(),
                story.id(),
                true,
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot block story for child");
    }

    private UserAccount saveUser(UserRole role, String email) {
        return userRepository.save(new UserAccount(
                UUID.randomUUID(),
                email,
                "hash",
                email,
                role,
                UserStatus.ACTIVE,
                NOW,
                NOW));
    }

    private Genre saveGenre(String name) {
        StoryTextProcessor processor = new StoryTextProcessor();
        return genreRepository.save(new Genre(UUID.randomUUID(), name, processor.normalizeForSearch(name), NOW));
    }

    private Author saveAuthor(String name) {
        StoryTextProcessor processor = new StoryTextProcessor();
        return authorRepository.save(new Author(UUID.randomUUID(), name, processor.normalizeForSearch(name), NOW));
    }

    private void saveAcceptedLink(UserAccount guardian, UserAccount child) {
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
}
