package com.blackbox.collector.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PushEventPayload(
        String ref,
        List<CommitInfo> commits,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitInfo(
            String id,
            String message,
            String timestamp,
            Author author
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
                    String name,
                    String email,
                    String username
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            @JsonProperty("full_name") String fullName,
            Long id
    ) {}
}
