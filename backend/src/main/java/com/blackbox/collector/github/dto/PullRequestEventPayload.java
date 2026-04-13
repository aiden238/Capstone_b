package com.blackbox.collector.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestEventPayload(
        String action,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            Long id,
            Long number,
            String title,
            User user,
            boolean merged,
            @JsonProperty("merged_at") String mergedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            String login
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            @JsonProperty("full_name") String fullName
    ) {}
}
