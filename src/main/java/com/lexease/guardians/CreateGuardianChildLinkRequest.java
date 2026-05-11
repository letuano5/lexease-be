package com.lexease.guardians;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuardianChildLinkRequest(
        @NotBlank @Email @Size(max = 320) String childEmail
) {
}
