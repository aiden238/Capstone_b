package com.blackbox.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GitHubUserMappingRequest(
        @NotNull UUID userId,
        @NotBlank String githubUsername
) {}
