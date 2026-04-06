package com.blackbox.score.controller;

import com.blackbox.auth.entity.UserRole;
import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.security.ProjectAccessChecker;
import com.blackbox.score.dto.WeightConfigResponse;
import com.blackbox.score.dto.WeightUpdateRequest;
import com.blackbox.score.service.WeightConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/weights")
public class WeightConfigController {

    private final WeightConfigService weightConfigService;
    private final ProjectAccessChecker accessChecker;

    public WeightConfigController(WeightConfigService weightConfigService,
                                  ProjectAccessChecker accessChecker) {
        this.weightConfigService = weightConfigService;
        this.accessChecker = accessChecker;
    }

    /**
     * 현재 가중치 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeightConfigResponse>> getWeights(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        WeightConfigResponse weights = weightConfigService.getWeights(projectId);
        return ResponseEntity.ok(ApiResponse.success(weights));
    }

    /**
     * 가중치 변경 (PROFESSOR / TA만 가능)
     */
    @PutMapping
    public ResponseEntity<ApiResponse<WeightConfigResponse>> updateWeights(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Valid @RequestBody WeightUpdateRequest request) {
        // PROFESSOR 또는 TA만
        if (user.getRole() != UserRole.PROFESSOR && user.getRole() != UserRole.TA) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        accessChecker.checkAnyMember(projectId, user.getUserId());
        WeightConfigResponse weights = weightConfigService.updateWeights(projectId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(weights));
    }
}
