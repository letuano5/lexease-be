package com.lexease.notifications;

import com.lexease.notifications.dtos.req.CreateReminderRequest;
import com.lexease.notifications.dtos.req.PatchReminderRequest;
import com.lexease.notifications.dtos.res.ReminderResponse;
import com.lexease.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ReminderController {
    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/reminders")
    ReminderResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReminderRequest request
    ) {
        return reminderService.create(principal.id(), principal.role(), request);
    }

    @GetMapping("/children/{childId}/reminders")
    List<ReminderResponse> listForChild(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId
    ) {
        return reminderService.listForChild(principal.id(), principal.role(), childId);
    }

    @PatchMapping("/reminders/{id}")
    ReminderResponse patch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PatchReminderRequest request
    ) {
        return reminderService.patch(principal.id(), principal.role(), id, request);
    }

    @DeleteMapping("/reminders/{id}")
    void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        reminderService.delete(principal.id(), principal.role(), id);
    }
}
