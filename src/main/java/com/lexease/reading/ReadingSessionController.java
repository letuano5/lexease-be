package com.lexease.reading;

import com.lexease.reading.dtos.req.StartReadingSessionRequest;
import com.lexease.reading.dtos.req.UpdateReadingProgressRequest;
import com.lexease.reading.dtos.res.ReadingSessionResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
public class ReadingSessionController {
    private final ReadingService readingService;

    public ReadingSessionController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @PostMapping
    ReadingSessionResponse start(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartReadingSessionRequest request
    ) {
        return readingService.start(principal.id(), principal.role(), request);
    }

    @GetMapping("/active")
    ReadingSessionResponse active(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID storyId,
            @RequestParam(required = false) String voice
    ) {
        return readingService.active(principal.id(), principal.role(), storyId, voice);
    }

    @GetMapping("/{id}")
    ReadingSessionResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return readingService.get(principal.id(), principal.role(), id);
    }

    @PatchMapping("/{id}/progress")
    ReadingSessionResponse updateProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReadingProgressRequest request
    ) {
        return readingService.updateProgress(principal.id(), principal.role(), id, request);
    }

    @PostMapping("/{id}/complete")
    ReadingSessionResponse complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return readingService.complete(principal.id(), principal.role(), id);
    }
}
