package com.blackbox.score.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WeightUpdateRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("1.00")
        BigDecimal weightGit,

        @NotNull @DecimalMin("0.00") @DecimalMax("1.00")
        BigDecimal weightDoc,

        @NotNull @DecimalMin("0.00") @DecimalMax("1.00")
        BigDecimal weightMeeting,

        @NotNull @DecimalMin("0.00") @DecimalMax("1.00")
        BigDecimal weightTask
) {}
