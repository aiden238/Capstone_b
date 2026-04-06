package com.blackbox.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID projectId,
        String projectName,
        String courseName,
        String semester,
        int memberCount,
        // 태스크 통계
        int taskTotal,
        int taskTodo,
        int taskInProgress,
        int taskDone,
        // 기여도 요약
        BigDecimal scoreAvg,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        // 경보
        long unreadAlertCount,
        long totalAlertCount,
        // 건강 상태: HEALTHY, WARNING, DANGER
        String healthStatus,
        OffsetDateTime lastActivityAt
) {}
