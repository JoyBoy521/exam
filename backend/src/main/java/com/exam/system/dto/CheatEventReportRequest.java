package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheatEventReportRequest(
        @NotBlank String type,
        @NotNull Integer durationSeconds,
        String detail
) {
}
