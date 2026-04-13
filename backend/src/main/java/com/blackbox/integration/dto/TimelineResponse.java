package com.blackbox.integration.dto;

import com.blackbox.activity.entity.ActivityLog;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TimelineResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        String userName,
        String source,
        String actionType,
        String metadata,
        String externalId,
        BigDecimal trustLevel,
        OffsetDateTime occurredAt
) {
    public static TimelineResponse from(ActivityLog log, String userName) {
        return new TimelineResponse(
                log.getId(),
                log.getProjectId(),
                log.getUserId(),
                userName,
                log.getSource().name(),
                log.getActionType().name(),
                log.getMetadata(),
                log.getExternalId(),
                log.getTrustLevel(),
                log.getOccurredAt()
        );
    }
}
