package com.blackbox.collector.drive.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.collector.drive.dto.DriveIntegrationResponse;
import com.blackbox.collector.drive.service.DriveIntegrationService;
import com.blackbox.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/integrations/drive")
public class DriveIntegrationController {

    private final DriveIntegrationService integrationService;

    public DriveIntegrationController(DriveIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /** GET /api/projects/{id}/integrations/drive — 연동 상태 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<DriveIntegrationResponse>> getIntegration(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                integrationService.getIntegration(projectId, user.getUserId())));
    }

    /** GET /api/projects/{id}/integrations/drive/auth — OAuth 인증 URL → 302 리디렉트 */
    @GetMapping("/auth")
    public ResponseEntity<Void> startOAuth(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        String authUrl = integrationService.buildAuthUrl(projectId, user.getUserId());
        return ResponseEntity.status(302)
                .location(URI.create(authUrl))
                .build();
    }

    /** DELETE /api/projects/{id}/integrations/drive — 연동 해제 (LEADER만) */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> disconnectDrive(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        integrationService.disconnectDrive(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
