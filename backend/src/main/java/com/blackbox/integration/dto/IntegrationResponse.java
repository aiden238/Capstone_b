package com.blackbox.integration.dto;

import com.blackbox.integration.entity.ExternalIntegration;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationResponse(
        UUID id,
        UUID projectId,
        String provider,
        String externalId,
        String externalName,
        String syncStatus,
        String errorMessage,
        OffsetDateTime lastSynced,
        OffsetDateTime createdAt
) {
    public static IntegrationResponse from(ExternalIntegration e) {
        return new IntegrationResponse(
                e.getId(),
                e.getProject().getId(),
                e.getProvider().name(),
                e.getExternalId(),
                e.getExternalName(),
                e.getSyncStatus().name(),
                e.getErrorMessage(),
                e.getLastSynced(),
                e.getCreatedAt()
        );
    }
}
