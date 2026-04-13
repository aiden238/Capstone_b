package com.blackbox.integration.controller;

import com.blackbox.integration.service.GoogleDriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class GoogleDriveWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveWebhookController.class);

    private final GoogleDriveService driveService;

    public GoogleDriveWebhookController(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    /**
     * Google Drive Push Notification 수신 엔드포인트.
     * Drive Changes: watch API가 파일 변경 시 이 엔드포인트로 POST를 보낸다.
     */
    @PostMapping("/google-drive")
    public ResponseEntity<Void> handleDrivePushNotification(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String state,
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String channelToken) {

        log.info("Google Drive push notification: channelId={}, state={}", channelId, state);

        try {
            driveService.handlePushNotification(channelId, resourceId, state);
        } catch (Exception e) {
            log.error("Failed to process Drive push notification", e);
        }

        // Drive Push Notification은 항상 200 반환해야 함 (그렇지 않으면 채널 비활성화)
        return ResponseEntity.ok().build();
    }
}
