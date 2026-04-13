package com.blackbox.collector.github.dto;

import com.blackbox.collector.github.entity.ProjectGithubIntegration;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GithubIntegrationResponse(
        UUID id,
        UUID projectId,
        Long installationId,
        String repoFullName,
        OffsetDateTime createdAt
) {
    public static GithubIntegrationResponse from(ProjectGithubIntegration e) {
        return new GithubIntegrationResponse(
                e.getId(),
                e.getProject().getId(),
                e.getInstallationId(),
                e.getRepoFullName(),
                e.getCreatedAt()
        );
    }
}
