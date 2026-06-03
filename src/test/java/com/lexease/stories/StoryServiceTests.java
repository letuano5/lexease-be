package com.lexease.stories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lexease.authors.Author;
import com.lexease.authors.AuthorRepository;
import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkRepository;
import com.lexease.guardians.GuardianChildLinkStatus;
import com.lexease.genres.Genre;
import com.lexease.genres.GenreRepository;
import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.PageResponse;
import com.lexease.stories.dtos.req.PatchStoryRequest;
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
    void guardianSearchShowsBlockedStoryForManagedChild() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin2b@example.com");
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-b@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child2b@example.com");
        Genre genre = saveGenre("Thiếu nhi B");
        Author author = saveAuthor("Nguyễn B");
        saveAcceptedLink(guardian, child);
        StoryDetailResponse story = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện phụ huynh vẫn thấy",
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
                guardian.getId(),
                UserRole.GUARDIAN,
                null,
                List.of(),
                List.of(),
                child.getId(),
                0,
                20);

        assertThat(response.items())
                .extracting(StorySummaryResponse::id)
                .contains(story.id());
        assertThat(response.items())
                .filteredOn(item -> item.id().equals(story.id()))
                .extracting(StorySummaryResponse::isBlockedForCurrentChild)
                .containsExactly(true);
    }

    @Test
    void guardianCanGetBlockedStoryForManagedChild() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin2c@example.com");
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-c@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child2c@example.com");
        Genre genre = saveGenre("Thiếu nhi C");
        Author author = saveAuthor("Nguyễn C");
        saveAcceptedLink(guardian, child);
        StoryDetailResponse story = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện xem log",
                "Nội dung truyện.",
                List.of(genre.getId()),
                List.of(author.getId()),
                StoryStatus.PUBLISHED));

        storyService.block(guardian.getId(), UserRole.GUARDIAN, new StoryAccessChangeRequest(
                child.getId(),
                story.id(),
                true,
                "Không phù hợp"));

        StoryDetailResponse response = storyService.get(
                guardian.getId(),
                UserRole.GUARDIAN,
                story.id(),
                child.getId());

        assertThat(response.id()).isEqualTo(story.id());
    }

    @Test
    void childCannotGetStoryBlockedByAcceptedGuardian() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin2d@example.com");
        UserAccount guardian = saveUser(UserRole.GUARDIAN, "guardian-d@example.com");
        UserAccount child = saveUser(UserRole.CHILD, "child2d@example.com");
        Genre genre = saveGenre("Thiếu nhi D");
        Author author = saveAuthor("Nguyễn D");
        saveAcceptedLink(guardian, child);
        StoryDetailResponse story = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện child không thấy",
                "Nội dung truyện.",
                List.of(genre.getId()),
                List.of(author.getId()),
                StoryStatus.PUBLISHED));

        storyService.block(guardian.getId(), UserRole.GUARDIAN, new StoryAccessChangeRequest(
                child.getId(),
                story.id(),
                true,
                "Không phù hợp"));

        assertThatThrownBy(() -> storyService.get(child.getId(), UserRole.CHILD, story.id(), child.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Story not found");
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

    @Test
    void patchStoryUpdatesOnlyProvidedFields() {
        UserAccount admin = saveUser(UserRole.ADMIN, "admin4@example.com");
        Genre oldGenre = saveGenre("Thiếu nhi");
        Genre newGenre = saveGenre("Phiêu lưu");
        Author author = saveAuthor("Nguyễn A");
        StoryDetailResponse created = storyService.create(admin.getId(), new StoryUpsertRequest(
                "Câu chuyện cũ",
                "Nội dung vẫn giữ nguyên.",
                List.of(oldGenre.getId()),
                List.of(author.getId()),
                StoryStatus.PUBLISHED));

        StoryDetailResponse updated = storyService.update(admin.getId(), created.id(), new PatchStoryRequest(
                null,
                null,
                List.of(newGenre.getId()),
                null,
                null));

        assertThat(updated.title()).isEqualTo("Câu chuyện cũ");
        assertThat(updated.content()).isEqualTo("Nội dung vẫn giữ nguyên.");
        assertThat(updated.status()).isEqualTo(StoryStatus.PUBLISHED);
        assertThat(updated.authors()).extracting("id").containsExactly(author.getId());
        assertThat(updated.genres()).extracting("id").containsExactly(newGenre.getId());
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
        return genreRepository.save(new Genre(UUID.randomUUID(), name, processor.normalizeForSearch(name), NOW, NOW));
    }

    private Author saveAuthor(String name) {
        StoryTextProcessor processor = new StoryTextProcessor();
        return authorRepository.save(new Author(UUID.randomUUID(), name, processor.normalizeForSearch(name), NOW, NOW));
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
