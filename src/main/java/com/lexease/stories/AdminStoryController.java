package com.lexease.stories;

import com.lexease.stories.dtos.req.PatchStoryRequest;
import com.lexease.stories.dtos.req.StoryUpsertRequest;
import com.lexease.stories.dtos.res.StoryDetailResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoryController {
    private final StoryService storyService;

    public AdminStoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StoryDetailResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoryUpsertRequest request
    ) {
        return storyService.create(principal.id(), request);
    }

    @PatchMapping("/{id}")
    StoryDetailResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PatchStoryRequest request
    ) {
        return storyService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        storyService.archive(principal.id(), id);
    }
}
