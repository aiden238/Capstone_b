package com.blackbox.collector.github.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.service.ActivityLogService;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.collector.github.dto.*;
import com.blackbox.collector.github.repository.ProjectGithubIntegrationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * GitHub Webhook 이벤트별 처리 서비스.
 * 각 이벤트를 activity_logs에 저장. GitHub username → userId 매핑 실패 시 조용히 스킵.
 */
@Service
@Transactional(readOnly = true)
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);
    private static final BigDecimal TRUST_LEVEL = new BigDecimal("1.00");

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final ProjectGithubIntegrationRepository integrationRepository;
    private final ObjectMapper objectMapper;

    public GitHubWebhookService(ActivityLogService activityLogService,
                                UserRepository userRepository,
                                ProjectGithubIntegrationRepository integrationRepository,
                                ObjectMapper objectMapper) {
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
        this.integrationRepository = integrationRepository;
        this.objectMapper = objectMapper;
    }

    /** push 이벤트 처리: 커밋별 COMMIT 로그 생성 */
    public void handlePush(String rawPayload) {
        PushEventPayload payload = parse(rawPayload, PushEventPayload.class);
        if (payload == null || payload.commits() == null) return;

        String repoFullName = payload.repository().fullName();
        UUID projectId = resolveProjectId(repoFullName);
        if (projectId == null) return;

        for (PushEventPayload.CommitInfo commit : payload.commits()) {
            String githubUsername = commit.author() != null ? commit.author().username() : null;
            Optional<UUID> userId = resolveUserId(githubUsername);
            if (userId.isEmpty()) {
                log.debug("GitHub username '{}' not mapped to any user, skipping commit {}", githubUsername, commit.id());
                continue;
            }
            String metadata = buildMetadata("commitMessage", commit.message(), "commitSha", commit.id());
            activityLogService.logExternal(
                    projectId, userId.get(),
                    ActivitySource.GITHUB, ActionType.COMMIT,
                    metadata,
                    "commit-" + commit.id(),
                    TRUST_LEVEL,
                    parseTimestamp(commit.timestamp())
            );
        }
    }

    /** pull_request 이벤트 처리: opened → PR_OPEN, closed+merged → PR_MERGE */
    public void handlePullRequest(String rawPayload) {
        PullRequestEventPayload payload = parse(rawPayload, PullRequestEventPayload.class);
        if (payload == null) return;

        String repoFullName = payload.repository().fullName();
        UUID projectId = resolveProjectId(repoFullName);
        if (projectId == null) return;

        String githubUsername = payload.pullRequest().user().login();
        Optional<UUID> userId = resolveUserId(githubUsername);
        if (userId.isEmpty()) return;

        ActionType actionType = switch (payload.action()) {
            case "opened" -> ActionType.PR_OPEN;
            case "closed" -> payload.pullRequest().merged() ? ActionType.PR_MERGE : null;
            default -> null;
        };
        if (actionType == null) return;

        String prId = "pr-" + payload.pullRequest().id() + "-" + payload.action();
        String metadata = buildMetadata("prTitle", payload.pullRequest().title(),
                "prNumber", String.valueOf(payload.pullRequest().number()));
        activityLogService.logExternal(
                projectId, userId.get(),
                ActivitySource.GITHUB, actionType,
                metadata, prId, TRUST_LEVEL, OffsetDateTime.now()
        );
    }

    /** issues 이벤트 처리: opened → ISSUE_CREATE, closed → ISSUE_CLOSE */
    public void handleIssues(String rawPayload) {
        IssuesEventPayload payload = parse(rawPayload, IssuesEventPayload.class);
        if (payload == null) return;

        String repoFullName = payload.repository().fullName();
        UUID projectId = resolveProjectId(repoFullName);
        if (projectId == null) return;

        String githubUsername = payload.issue().user().login();
        Optional<UUID> userId = resolveUserId(githubUsername);
        if (userId.isEmpty()) return;

        ActionType actionType = switch (payload.action()) {
            case "opened" -> ActionType.ISSUE_CREATE;
            case "closed" -> ActionType.ISSUE_CLOSE;
            default -> null;
        };
        if (actionType == null) return;

        String issueId = "issue-" + payload.issue().id() + "-" + payload.action();
        String metadata = buildMetadata("issueTitle", payload.issue().title(),
                "issueNumber", String.valueOf(payload.issue().number()));
        activityLogService.logExternal(
                projectId, userId.get(),
                ActivitySource.GITHUB, actionType,
                metadata, issueId, TRUST_LEVEL, OffsetDateTime.now()
        );
    }

    /** pull_request_review 이벤트 처리: submitted → CODE_REVIEW */
    public void handlePullRequestReview(String rawPayload) {
        PullRequestReviewEventPayload payload = parse(rawPayload, PullRequestReviewEventPayload.class);
        if (payload == null || !"submitted".equals(payload.action())) return;

        String repoFullName = payload.repository().fullName();
        UUID projectId = resolveProjectId(repoFullName);
        if (projectId == null) return;

        String githubUsername = payload.review().user().login();
        Optional<UUID> userId = resolveUserId(githubUsername);
        if (userId.isEmpty()) return;

        String reviewId = "review-" + payload.review().id();
        String metadata = buildMetadata("prTitle", payload.pullRequest().title(),
                "reviewState", payload.review().state());
        activityLogService.logExternal(
                projectId, userId.get(),
                ActivitySource.GITHUB, ActionType.CODE_REVIEW,
                metadata, reviewId, TRUST_LEVEL,
                parseTimestamp(payload.review().submittedAt())
        );
    }

    // ── helpers ──────────────────────────────────────────────

    private UUID resolveProjectId(String repoFullName) {
        return integrationRepository.findByRepoFullName(repoFullName)
                .map(i -> i.getProject().getId())
                .orElse(null);
    }

    private Optional<UUID> resolveUserId(String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) return Optional.empty();
        return userRepository.findByGithubUsername(githubUsername).map(u -> u.getId());
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse GitHub webhook payload as {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private OffsetDateTime parseTimestamp(String iso8601) {
        if (iso8601 == null) return OffsetDateTime.now();
        try {
            return OffsetDateTime.parse(iso8601);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }

    private String buildMetadata(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(keyValues[i]).append("\":\"")
              .append(keyValues[i + 1] != null ? keyValues[i + 1].replace("\"", "\\\"") : "").append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
