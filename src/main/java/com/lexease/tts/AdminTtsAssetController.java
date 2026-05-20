package com.lexease.tts;

import com.lexease.tts.dtos.req.GenerateTtsAssetRequest;
import com.lexease.tts.dtos.res.TtsAssetGenerationResult;
import com.lexease.tts.dtos.res.TtsAssetResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stories/{storyId}/tts-assets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTtsAssetController {
    private final TtsService ttsService;

    public AdminTtsAssetController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping
    ResponseEntity<TtsAssetResponse> generate(
            @PathVariable UUID storyId,
            @Valid @RequestBody GenerateTtsAssetRequest request
    ) {
        TtsAssetGenerationResult result = ttsService.generateAsset(
                storyId,
                request.voice(),
                request.refreshRequested());
        HttpStatus status = result.accepted() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    List<TtsAssetResponse> list(@PathVariable UUID storyId) {
        return ttsService.listAssets(storyId);
    }
}
