package com.blackbox.integration.dto;

import com.blackbox.integration.entity.GitHubUserMapping;

import java.util.UUID;

public record GitHubUserMappingResponse(
        UUID id,
        UUID userId,
        String userName,
        String githubUsername,
        Long githubId
) {
    public static GitHubUserMappingResponse from(GitHubUserMapping m) {
        return new GitHubUserMappingResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getGithubUsername(),
                m.getGithubId()
        );
    }
}
