package com.blackbox.collector.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestReviewEventPayload(
        String action,
        Review review,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(
            Long id,
            String state,
            User user,
            @JsonProperty("submitted_at") String submittedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            Long number,
            String title
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
