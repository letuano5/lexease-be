package com.lexease.guardians;

import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guardian-child-links")
public class GuardianChildLinkController {
    private final GuardianChildLinkService guardianChildLinkService;

    public GuardianChildLinkController(GuardianChildLinkService guardianChildLinkService) {
        this.guardianChildLinkService = guardianChildLinkService;
    }

    @PostMapping
    GuardianChildLinkResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGuardianChildLinkRequest request
    ) {
        return guardianChildLinkService.create(principal, request);
    }

    @GetMapping
    List<GuardianChildLinkResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return guardianChildLinkService.list(principal);
    }

    @PostMapping("/{id}/accept")
    GuardianChildLinkResponse accept(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return guardianChildLinkService.accept(principal, id);
    }

    @PostMapping("/{id}/reject")
    GuardianChildLinkResponse reject(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return guardianChildLinkService.reject(principal, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        guardianChildLinkService.revoke(principal, id);
    }
}
