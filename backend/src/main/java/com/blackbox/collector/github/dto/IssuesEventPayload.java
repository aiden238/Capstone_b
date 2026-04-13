package com.blackbox.collector.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssuesEventPayload(
        String action,
        Issue issue,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(
            Long id,
            Long number,
            String title,
            User user,
            @JsonProperty("closed_at") String closedAt
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
