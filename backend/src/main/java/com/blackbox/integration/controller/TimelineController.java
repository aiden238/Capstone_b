package com.blackbox.integration.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.integration.dto.TimelineResponse;
import com.blackbox.integration.service.TimelineService;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/timeline")
public class TimelineController {

    private final TimelineService timelineService;
    private final ProjectAccessChecker accessChecker;

    public TimelineController(TimelineService timelineService,
                               ProjectAccessChecker accessChecker) {
        this.timelineService = timelineService;
        this.accessChecker = accessChecker;
    }

    /**
     * 프로젝트 통합 타임라인 조회
     *
     * @param source 필터: PLATFORM, GITHUB, GOOGLE_DRIVE (선택)
     * @param userId 특정 사용자 필터 (선택)
     * @param page   페이지 번호 (0부터)
     * @param size   페이지 크기
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TimelineResponse>>> getTimeline(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        accessChecker.checkAnyMember(projectId, user.getUserId());

        List<TimelineResponse> timeline = timelineService.getTimeline(projectId, userId, source, page, size);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }
}
