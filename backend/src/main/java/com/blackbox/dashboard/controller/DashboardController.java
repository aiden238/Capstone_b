package com.blackbox.dashboard.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.dashboard.dto.ProjectSummaryResponse;
import com.blackbox.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 내가 참여 중인 모든 프로젝트 요약 (교수 대시보드)
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<ProjectSummaryResponse>>> getOverview(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<ProjectSummaryResponse> overview = dashboardService.getOverview(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
