package com.exam.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ManualReviewRequest(
        @NotNull @DecimalMin("0.0") Double manualScore
) {
}
