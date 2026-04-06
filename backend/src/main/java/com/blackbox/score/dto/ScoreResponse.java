package com.blackbox.score.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScoreResponse(
        UUID userId,
        String userName,
        String email,
        BigDecimal taskScore,
        BigDecimal meetingScore,
        BigDecimal docScore,
        BigDecimal gitScore,
        BigDecimal totalScore,
        OffsetDateTime calculatedAt
) {}
