package com.lexease.display;

import com.lexease.display.dtos.req.SaveDisplaySettingsRequest;
import com.lexease.display.dtos.res.DisplaySettingsResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/children/{childId}/display-settings")
public class DisplaySettingsController {
    private final DisplaySettingsService displaySettingsService;

    public DisplaySettingsController(DisplaySettingsService displaySettingsService) {
        this.displaySettingsService = displaySettingsService;
    }

    @GetMapping
    DisplaySettingsResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId
    ) {
        return displaySettingsService.get(principal.id(), principal.role(), childId);
    }

    @PutMapping
    DisplaySettingsResponse save(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId,
            @Valid @RequestBody SaveDisplaySettingsRequest request
    ) {
        return displaySettingsService.save(principal.id(), principal.role(), childId, request);
    }

    @PostMapping("/reset")
    DisplaySettingsResponse reset(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId
    ) {
        return displaySettingsService.reset(principal.id(), principal.role(), childId);
    }
}
