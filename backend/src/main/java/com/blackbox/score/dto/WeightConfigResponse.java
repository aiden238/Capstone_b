package com.blackbox.score.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WeightConfigResponse(
        UUID id,
        UUID projectId,
        BigDecimal weightGit,
        BigDecimal weightDoc,
        BigDecimal weightMeeting,
        BigDecimal weightTask,
        OffsetDateTime updatedAt
) {}
