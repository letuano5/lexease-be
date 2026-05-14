package com.lexease.stories;

import com.lexease.shared.api.PageResponse;
import com.lexease.shared.security.UserPrincipal;
import com.lexease.stories.dtos.res.StoryDetailResponse;
import com.lexease.stories.dtos.res.StorySummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stories")
public class StoryController {
    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping
    PageResponse<StorySummaryResponse> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "genreId") List<UUID> genreIds,
            @RequestParam(required = false, name = "authorId") List<UUID> authorIds,
            @RequestParam(required = false) UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return storyService.search(
                principal.id(),
                principal.role(),
                keyword,
                genreIds == null ? List.of() : genreIds,
                authorIds == null ? List.of() : authorIds,
                childId,
                page,
                size);
    }

    @GetMapping("/{id}")
    StoryDetailResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID childId
    ) {
        return storyService.get(principal.id(), principal.role(), id, childId);
    }
}
