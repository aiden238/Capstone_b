package com.blackbox.integration.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.integration.dto.*;
import com.blackbox.integration.service.IntegrationService;
import com.blackbox.project.security.ProjectAccessChecker;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;
    private final ProjectAccessChecker accessChecker;

    public IntegrationController(IntegrationService integrationService,
                                  ProjectAccessChecker accessChecker) {
        this.integrationService = integrationService;
        this.accessChecker = accessChecker;
    }

    /** 외부 서비스 연동 추가 (LEADER만 가능) */
    @PostMapping
    public ResponseEntity<ApiResponse<IntegrationResponse>> createIntegration(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateIntegrationRequest request) {
        accessChecker.checkLeader(projectId, user.getUserId());
        IntegrationResponse response = integrationService.createIntegration(projectId, user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 프로젝트의 모든 연동 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IntegrationResponse>>> getIntegrations(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        List<IntegrationResponse> response = integrationService.getIntegrations(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 연동 삭제 (LEADER만 가능) */
    @DeleteMapping("/{integrationId}")
    public ResponseEntity<ApiResponse<Void>> deleteIntegration(
            @PathVariable UUID projectId,
            @PathVariable UUID integrationId,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkLeader(projectId, user.getUserId());
        integrationService.deleteIntegration(projectId, integrationId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 연동 상태 변경 */
    @PatchMapping("/{integrationId}/status")
    public ResponseEntity<ApiResponse<IntegrationResponse>> updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID integrationId,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkLeader(projectId, user.getUserId());
        IntegrationResponse response = integrationService.updateSyncStatus(projectId, integrationId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- GitHub User Mapping ---

    /** GitHub 사용자 매핑 추가 */
    @PostMapping("/github-mappings")
    public ResponseEntity<ApiResponse<GitHubUserMappingResponse>> createMapping(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody GitHubUserMappingRequest request) {
        accessChecker.checkMemberOrAbove(projectId, user.getUserId());
        GitHubUserMappingResponse response = integrationService.createMapping(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** GitHub 사용자 매핑 목록 */
    @GetMapping("/github-mappings")
    public ResponseEntity<ApiResponse<List<GitHubUserMappingResponse>>> getMappings(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        List<GitHubUserMappingResponse> response = integrationService.getMappings(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GitHub 사용자 매핑 삭제 */
    @DeleteMapping("/github-mappings/{mappingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(
            @PathVariable UUID projectId,
            @PathVariable UUID mappingId,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkMemberOrAbove(projectId, user.getUserId());
        integrationService.deleteMapping(projectId, mappingId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
