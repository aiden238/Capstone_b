package com.blackbox.alert.dto;

import com.blackbox.alert.entity.AlertSeverity;
import com.blackbox.alert.entity.AlertType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        AlertType alertType,
        AlertSeverity severity,
        String message,
        boolean isRead,
        OffsetDateTime createdAt
) {}
