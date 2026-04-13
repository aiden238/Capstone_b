package com.blackbox.integration.controller;

import com.blackbox.integration.service.GitHubWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final GitHubWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${github.webhook-secret:}")
    private String webhookSecret;

    public GitHubWebhookController(GitHubWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String payload) {

        log.info("GitHub webhook received: event={}, deliveryId={}", event, deliveryId);

        // 서명 검증 (webhook secret이 설정된 경우에만)
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (!webhookService.verifySignature(payload, signature, webhookSecret)) {
                log.warn("GitHub webhook signature verification failed for delivery: {}", deliveryId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
        }

        if (event == null) {
            return ResponseEntity.badRequest().body("Missing X-GitHub-Event header");
        }

        try {
            JsonNode payloadNode = objectMapper.readTree(payload);

            switch (event) {
                case "push" -> webhookService.handlePushEvent(payloadNode);
                case "pull_request" -> webhookService.handlePullRequestEvent(payloadNode);
                case "issues" -> webhookService.handleIssuesEvent(payloadNode);
                case "pull_request_review" -> webhookService.handlePullRequestReviewEvent(payloadNode);
                case "ping" -> log.info("GitHub webhook ping received");
                default -> log.debug("Unhandled GitHub event: {}", event);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Failed to process GitHub webhook: event={}", event, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
        }
    }
}
