package com.lexease.progress;

import com.lexease.progress.dtos.res.DifficultWordProgressResponse;
import com.lexease.progress.dtos.res.ProgressBucketResponse;
import com.lexease.progress.dtos.res.ProgressSessionDetailResponse;
import com.lexease.progress.dtos.res.ProgressSessionResponse;
import com.lexease.progress.dtos.res.ProgressSummaryResponse;
import com.lexease.shared.security.UserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/children/{childId}/progress")
public class ProgressController {
    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/summary")
    ProgressSummaryResponse summary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "week") String range
    ) {
        return progressService.summary(principal.id(), principal.role(), childId, range);
    }

    @GetMapping("/timeseries")
    List<ProgressBucketResponse> timeseries(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "week") String range
    ) {
        return progressService.timeseries(principal.id(), principal.role(), childId, range);
    }

    @GetMapping("/difficult-words")
    List<DifficultWordProgressResponse> difficultWords(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "month") String range
    ) {
        return progressService.difficultWords(principal.id(), principal.role(), childId, range);
    }

    @GetMapping("/sessions")
    List<ProgressSessionResponse> sessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "month") String range
    ) {
        return progressService.sessions(principal.id(), principal.role(), childId, range);
    }

    @GetMapping("/sessions/{sessionId}")
    ProgressSessionDetailResponse sessionDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @PathVariable UUID sessionId
    ) {
        return progressService.sessionDetail(principal.id(), principal.role(), childId, sessionId);
    }
}
