package com.blackbox.collector.github.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.collector.github.dto.ConnectGithubRequest;
import com.blackbox.collector.github.dto.GithubIntegrationResponse;
import com.blackbox.collector.github.service.GitHubIntegrationService;
import com.blackbox.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/integrations/github")
public class GitHubIntegrationController {

    private final GitHubIntegrationService integrationService;

    public GitHubIntegrationController(GitHubIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /** GET /api/projects/{id}/integrations/github — 연동 상태 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<GithubIntegrationResponse>> getIntegration(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                integrationService.getIntegration(projectId, user.getUserId())));
    }

    /** POST /api/projects/{id}/integrations/github — 저장소 연동 (LEADER만) */
    @PostMapping
    public ResponseEntity<ApiResponse<GithubIntegrationResponse>> connectRepo(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ConnectGithubRequest request) {
        GithubIntegrationResponse response = integrationService.connectRepo(projectId, user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** DELETE /api/projects/{id}/integrations/github — 연동 해제 (LEADER만) */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> disconnectRepo(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        integrationService.disconnectRepo(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
