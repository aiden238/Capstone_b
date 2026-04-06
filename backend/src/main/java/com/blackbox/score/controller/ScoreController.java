package com.blackbox.score.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.project.security.ProjectAccessChecker;
import com.blackbox.score.dto.ScoreResponse;
import com.blackbox.score.service.ScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/scores")
public class ScoreController {

    private final ScoreService scoreService;
    private final ProjectAccessChecker accessChecker;

    public ScoreController(ScoreService scoreService, ProjectAccessChecker accessChecker) {
        this.scoreService = scoreService;
        this.accessChecker = accessChecker;
    }

    /**
     * 프로젝트 전체 팀원 기여도 점수 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> getScores(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        List<ScoreResponse> scores = scoreService.getScores(projectId);
        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    /**
     * 내 기여도 점수 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ScoreResponse>> getMyScore(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        ScoreResponse score = scoreService.getMyScore(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    /**
     * 점수 재계산 (수동 트리거)
     */
    @PostMapping("/recalculate")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> recalculate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        List<ScoreResponse> scores = scoreService.calculateAndSave(projectId);
        return ResponseEntity.ok(ApiResponse.success(scores));
    }
}
