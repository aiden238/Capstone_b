package com.blackbox.collector.github.controller;

import com.blackbox.collector.github.service.GitHubWebhookService;
import com.blackbox.collector.github.service.GitHubWebhookVerifier;
import com.blackbox.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub App Webhook 수신 엔드포인트.
 * Nginx: /api/webhooks/ → backend (already configured)
 */
@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final GitHubWebhookService webhookService;
    private final GitHubWebhookVerifier verifier;

    public GitHubWebhookController(GitHubWebhookService webhookService,
                                   GitHubWebhookVerifier verifier) {
        this.webhookService = webhookService;
        this.verifier = verifier;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] rawPayload
    ) {
        if (!verifier.verify(rawPayload, signature)) {
            log.warn("Invalid GitHub webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("GITHUB_003", "Webhook 서명이 유효하지 않습니다"));
        }

        if (event == null) {
            return ResponseEntity.ok(ApiResponse.success());
        }

        String payloadStr = new String(rawPayload, java.nio.charset.StandardCharsets.UTF_8);

        switch (event) {
            case "push" -> webhookService.handlePush(payloadStr);
            case "pull_request" -> webhookService.handlePullRequest(payloadStr);
            case "issues" -> webhookService.handleIssues(payloadStr);
            case "pull_request_review" -> webhookService.handlePullRequestReview(payloadStr);
            case "ping" -> log.info("GitHub webhook ping received");
            default -> log.debug("Unhandled GitHub event: {}", event);
        }

        return ResponseEntity.ok(ApiResponse.success());
    }
}
