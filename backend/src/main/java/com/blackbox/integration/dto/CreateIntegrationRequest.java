package com.blackbox.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIntegrationRequest(
        @NotNull String provider,       // GITHUB_APP | GOOGLE_DRIVE
        @NotBlank String externalId,    // owner/repo | Drive folder ID
        String externalName,            // display name
        Long installationId             // GitHub App installation ID
) {}
