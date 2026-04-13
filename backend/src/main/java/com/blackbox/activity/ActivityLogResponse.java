package com.blackbox.activity;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivitySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        ActivitySource source,
        ActionType actionType,
        String metadata,
        String externalId,
        BigDecimal trustLevel,
        OffsetDateTime occurredAt,
        BigDecimal qualityScore,
        String qualityReason,
        String analysisMethod
) {
    public static ActivityLogResponse from(ActivityLog e) {
        return new ActivityLogResponse(
                e.getId(),
                e.getProjectId(),
                e.getUserId(),
                e.getSource(),
                e.getActionType(),
                e.getMetadata(),
                e.getExternalId(),
                e.getTrustLevel(),
                e.getOccurredAt(),
                e.getQualityScore(),
                e.getQualityReason(),
                e.getAnalysisMethod()
        );
    }
}
