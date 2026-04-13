package com.blackbox.integration.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.integration.entity.ExternalIntegration;
import com.blackbox.integration.entity.GitHubUserMapping;
import com.blackbox.integration.entity.IntegrationProvider;
import com.blackbox.integration.entity.SyncStatus;
import com.blackbox.integration.repository.ExternalIntegrationRepository;
import com.blackbox.integration.repository.GitHubUserMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);

    private final ExternalIntegrationRepository integrationRepository;
    private final GitHubUserMappingRepository mappingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public GitHubWebhookService(ExternalIntegrationRepository integrationRepository,
                                 GitHubUserMappingRepository mappingRepository,
                                 ActivityLogRepository activityLogRepository,
                                 ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.mappingRepository = mappingRepository;
        this.activityLogRepository = activityLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * GitHub Webhook 서명 검증
     */
    public boolean verifySignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + bytesToHex(hash);
            return expected.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    /**
     * push 이벤트 처리 — 커밋 데이터 수집
     */
    @Transactional
    public void handlePushEvent(JsonNode payload) {
        String repoFullName = payload.path("repository").path("full_name").asText();
        JsonNode commitsNode = payload.path("commits");

        List<ExternalIntegration> integrations = findIntegrationsByRepo(repoFullName);
        if (integrations.isEmpty()) {
            log.debug("No integration found for repo: {}", repoFullName);
            return;
        }

        for (ExternalIntegration integration : integrations) {
            UUID projectId = integration.getProject().getId();

            for (JsonNode commitNode : commitsNode) {
                String authorUsername = commitNode.path("author").path("username").asText(null);
                if (authorUsername == null) {
                    authorUsername = commitNode.path("committer").path("username").asText(null);
                }

                UUID userId = resolveUserId(projectId, authorUsername);
                if (userId == null) {
                    log.debug("Unmapped GitHub user: {} in project {}", authorUsername, projectId);
                    continue;
                }

                String commitId = commitNode.path("id").asText();

                // 중복 체크
                if (isDuplicate(projectId, commitId)) continue;

                String message = commitNode.path("message").asText("");
                int addedCount = commitNode.path("added").size();
                int modifiedCount = commitNode.path("modified").size();
                int removedCount = commitNode.path("removed").size();

                String metadata = toJson(Map.of(
                        "commitId", commitId.substring(0, Math.min(7, commitId.length())),
                        "message", truncate(message, 200),
                        "added", addedCount,
                        "modified", modifiedCount,
                        "removed", removedCount,
                        "repo", repoFullName
                ));

                OffsetDateTime occurredAt = parseTimestamp(commitNode.path("timestamp").asText(null));

                ActivityLog activityLog = new ActivityLog(projectId, userId, ActivitySource.GITHUB, ActionType.COMMIT, metadata);
                activityLog.setExternalId(commitId);
                activityLog.setOccurredAt(occurredAt != null ? occurredAt : OffsetDateTime.now());
                activityLogRepository.save(activityLog);
            }

            updateLastSynced(integration);
        }
    }

    /**
     * pull_request 이벤트 처리
     */
    @Transactional
    public void handlePullRequestEvent(JsonNode payload) {
        String action = payload.path("action").asText();
        JsonNode prNode = payload.path("pull_request");
        String repoFullName = payload.path("repository").path("full_name").asText();

        ActionType actionType;
        switch (action) {
            case "opened", "reopened" -> actionType = ActionType.PR_OPEN;
            case "closed" -> {
                boolean merged = prNode.path("merged").asBoolean(false);
                actionType = merged ? ActionType.PR_MERGE : ActionType.PR_OPEN;
                if (!merged) return; // closed without merge — skip
            }
            default -> {
                return; // ignore other actions
            }
        }

        List<ExternalIntegration> integrations = findIntegrationsByRepo(repoFullName);
        for (ExternalIntegration integration : integrations) {
            UUID projectId = integration.getProject().getId();
            String authorUsername = prNode.path("user").path("login").asText(null);
            UUID userId = resolveUserId(projectId, authorUsername);
            if (userId == null) continue;

            String externalId = "pr-" + prNode.path("number").asInt();
            if (isDuplicate(projectId, externalId + "-" + action)) continue;

            String metadata = toJson(Map.of(
                    "prNumber", prNode.path("number").asInt(),
                    "title", truncate(prNode.path("title").asText(""), 200),
                    "action", action,
                    "merged", prNode.path("merged").asBoolean(false),
                    "additions", prNode.path("additions").asInt(0),
                    "deletions", prNode.path("deletions").asInt(0),
                    "repo", repoFullName
            ));

            OffsetDateTime occurredAt = parseTimestamp(prNode.path("created_at").asText(null));

            ActivityLog log = new ActivityLog(projectId, userId, ActivitySource.GITHUB, actionType, metadata);
            log.setExternalId(externalId + "-" + action);
            log.setOccurredAt(occurredAt != null ? occurredAt : OffsetDateTime.now());
            activityLogRepository.save(log);

            updateLastSynced(integration);
        }
    }

    /**
     * issues 이벤트 처리
     */
    @Transactional
    public void handleIssuesEvent(JsonNode payload) {
        String action = payload.path("action").asText();
        JsonNode issueNode = payload.path("issue");
        String repoFullName = payload.path("repository").path("full_name").asText();

        ActionType actionType;
        switch (action) {
            case "opened" -> actionType = ActionType.ISSUE_CREATE;
            case "closed" -> actionType = ActionType.ISSUE_CLOSE;
            default -> {
                return;
            }
        }

        List<ExternalIntegration> integrations = findIntegrationsByRepo(repoFullName);
        for (ExternalIntegration integration : integrations) {
            UUID projectId = integration.getProject().getId();
            String authorUsername = issueNode.path("user").path("login").asText(null);
            UUID userId = resolveUserId(projectId, authorUsername);
            if (userId == null) continue;

            String externalId = "issue-" + issueNode.path("number").asInt() + "-" + action;
            if (isDuplicate(projectId, externalId)) continue;

            String metadata = toJson(Map.of(
                    "issueNumber", issueNode.path("number").asInt(),
                    "title", truncate(issueNode.path("title").asText(""), 200),
                    "action", action,
                    "labels", extractLabels(issueNode.path("labels")),
                    "repo", repoFullName
            ));

            ActivityLog log = new ActivityLog(projectId, userId, ActivitySource.GITHUB, actionType, metadata);
            log.setExternalId(externalId);
            activityLogRepository.save(log);

            updateLastSynced(integration);
        }
    }

    /**
     * pull_request_review 이벤트 처리 — 코드 리뷰
     */
    @Transactional
    public void handlePullRequestReviewEvent(JsonNode payload) {
        String action = payload.path("action").asText();
        if (!"submitted".equals(action)) return;

        JsonNode reviewNode = payload.path("review");
        JsonNode prNode = payload.path("pull_request");
        String repoFullName = payload.path("repository").path("full_name").asText();

        List<ExternalIntegration> integrations = findIntegrationsByRepo(repoFullName);
        for (ExternalIntegration integration : integrations) {
            UUID projectId = integration.getProject().getId();
            String reviewerUsername = reviewNode.path("user").path("login").asText(null);
            UUID userId = resolveUserId(projectId, reviewerUsername);
            if (userId == null) continue;

            String externalId = "review-" + reviewNode.path("id").asLong();
            if (isDuplicate(projectId, externalId)) continue;

            String metadata = toJson(Map.of(
                    "prNumber", prNode.path("number").asInt(),
                    "state", reviewNode.path("state").asText(""),
                    "body", truncate(reviewNode.path("body").asText(""), 200),
                    "repo", repoFullName
            ));

            ActivityLog log = new ActivityLog(projectId, userId, ActivitySource.GITHUB, ActionType.CODE_REVIEW, metadata);
            log.setExternalId(externalId);
            activityLogRepository.save(log);

            updateLastSynced(integration);
        }
    }

    // --- Helper methods ---

    private List<ExternalIntegration> findIntegrationsByRepo(String repoFullName) {
        return integrationRepository.findByProviderAndExternalId(IntegrationProvider.GITHUB_APP, repoFullName)
                .filter(i -> i.getSyncStatus() == SyncStatus.ACTIVE)
                .stream().toList();
    }

    private UUID resolveUserId(UUID projectId, String githubUsername) {
        if (githubUsername == null) return null;
        return mappingRepository.findByProjectIdAndGithubUsername(projectId, githubUsername)
                .map(m -> m.getUser().getId())
                .orElse(null);
    }

    private boolean isDuplicate(UUID projectId, String externalId) {
        return activityLogRepository.existsByProjectIdAndExternalId(projectId, externalId);
    }

    private void updateLastSynced(ExternalIntegration integration) {
        integration.setLastSynced(OffsetDateTime.now());
        integrationRepository.save(integration);
    }

    private List<String> extractLabels(JsonNode labelsNode) {
        List<String> labels = new ArrayList<>();
        if (labelsNode != null && labelsNode.isArray()) {
            for (JsonNode label : labelsNode) {
                labels.add(label.path("name").asText());
            }
        }
        return labels;
    }

    private OffsetDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String str, int maxLen) {
        return str != null && str.length() > maxLen ? str.substring(0, maxLen) : str;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
