package com.blackbox.collector.github.dto;

public record ConnectGithubRequest(
        Long installationId,
        String repoFullName
) {}
