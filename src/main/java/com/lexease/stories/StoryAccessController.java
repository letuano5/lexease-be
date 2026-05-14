package com.lexease.stories;

import com.lexease.shared.security.UserPrincipal;
import com.lexease.stories.dtos.req.StoryAccessChangeRequest;
import com.lexease.stories.dtos.res.StoryAccessResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/story-access")
public class StoryAccessController {
    private final StoryService storyService;

    public StoryAccessController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping("/block")
    StoryAccessResponse block(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoryAccessChangeRequest request
    ) {
        return storyService.block(principal.id(), principal.role(), request);
    }

    @PostMapping("/unblock")
    StoryAccessResponse unblock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoryAccessChangeRequest request
    ) {
        return storyService.unblock(principal.id(), principal.role(), request);
    }
}
