package com.blackbox.activity;

import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/activities")
public class ActivityController {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectAccessChecker accessChecker;

    public ActivityController(ActivityLogRepository activityLogRepository,
                              ProjectAccessChecker accessChecker) {
        this.activityLogRepository = activityLogRepository;
        this.accessChecker = accessChecker;
    }

    /**
     * GET /api/projects/{projectId}/activities?page=0&size=30
     * 프로젝트 전체 활동 타임라인 (페이지네이션)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getActivities(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        accessChecker.checkAnyMember(projectId, user.getUserId());
        Page<ActivityLogResponse> logs = activityLogRepository
                .findAllByProjectIdOrderByOccurredAtDesc(
                        projectId,
                        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "occurredAt"))
                )
                .map(ActivityLogResponse::from);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
