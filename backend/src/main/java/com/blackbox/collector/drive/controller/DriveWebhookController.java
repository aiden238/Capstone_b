package com.blackbox.collector.drive.controller;

import com.blackbox.collector.drive.service.DriveIntegrationService;
import com.blackbox.collector.drive.service.DriveWebhookService;
import com.blackbox.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Google Drive Push Notification 수신 엔드포인트.
 * Nginx: /api/webhooks/ → backend (already configured)
 */
@RestController
@RequestMapping("/api/webhooks/drive")
public class DriveWebhookController {

    private static final Logger log = LoggerFactory.getLogger(DriveWebhookController.class);

    private final DriveWebhookService webhookService;
    private final DriveIntegrationService integrationService;

    public DriveWebhookController(DriveWebhookService webhookService,
                                  DriveIntegrationService integrationService) {
        this.webhookService = webhookService;
        this.integrationService = integrationService;
    }

    /** POST /api/webhooks/drive — Drive Change Notification */
    @PostMapping
    public ResponseEntity<Void> receiveNotification(
            @RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-Id", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState
    ) {
        if (channelId == null || resourceState == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            webhookService.handleNotification(channelId, resourceId, resourceState);
        } catch (Exception e) {
            log.error("Error processing Drive notification: channel={}", channelId, e);
        }
        // Drive Push Notification은 반드시 200 응답 (재시도 방지)
        return ResponseEntity.ok().build();
    }

    /** GET /api/oauth/google/callback — OAuth 콜백 */
    @RestController
    @RequestMapping("/api/oauth/google")
    public static class GoogleOAuthCallbackController {

        private final DriveIntegrationService integrationService;

        public GoogleOAuthCallbackController(DriveIntegrationService integrationService) {
            this.integrationService = integrationService;
        }

        @GetMapping("/callback")
        public ResponseEntity<ApiResponse<Void>> callback(
                @RequestParam String code,
                @RequestParam String state     // projectId
        ) {
            UUID projectId = UUID.fromString(state);
            integrationService.handleOAuthCallback(code, projectId);
            // 연동 완료 → settings 페이지로 리디렉트
            return ResponseEntity.status(302)
                    .header("Location", "/projects/" + projectId + "/settings")
                    .build();
        }
    }
}
