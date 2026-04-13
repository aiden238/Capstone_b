package com.blackbox.collector.drive.dto;

import com.blackbox.collector.drive.entity.ProjectDriveIntegration;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DriveIntegrationResponse(
        UUID id,
        UUID projectId,
        String driveFolderId,
        String watchChannelId,
        OffsetDateTime watchExpiry,
        OffsetDateTime createdAt
) {
    public static DriveIntegrationResponse from(ProjectDriveIntegration e) {
        return new DriveIntegrationResponse(
                e.getId(),
                e.getProject().getId(),
                e.getDriveFolderId(),
                e.getWatchChannelId(),
                e.getWatchExpiry(),
                e.getCreatedAt()
        );
    }
}
