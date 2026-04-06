package com.blackbox.alert.controller;

import com.blackbox.alert.dto.AlertResponse;
import com.blackbox.alert.service.AlertService;
import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/alerts")
public class AlertController {

    private final AlertService alertService;
    private final ProjectAccessChecker accessChecker;

    public AlertController(AlertService alertService, ProjectAccessChecker accessChecker) {
        this.alertService = alertService;
        this.accessChecker = accessChecker;
    }

    /**
     * 전체 경보 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(alertService.getAlerts(projectId)));
    }

    /**
     * 읽지 않은 경보 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getUnreadAlerts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(alertService.getUnreadAlerts(projectId)));
    }

    /**
     * 읽지 않은 경보 카운트
     */
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        long count = alertService.getUnreadCount(projectId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    /**
     * 경보 읽음 처리
     */
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @PathVariable UUID alertId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        alertService.markAsRead(alertId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 전체 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        alertService.markAllAsRead(projectId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 경보 감지 수동 실행
     */
    @PostMapping("/detect")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> runDetection(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(alertService.runDetection(projectId)));
    }
}
