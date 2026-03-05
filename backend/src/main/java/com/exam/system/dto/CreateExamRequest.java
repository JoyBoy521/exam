package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExamRequest(
        @NotNull Long paperId,
        @NotBlank String title,
        @NotBlank String startTime,
        @NotBlank String endTime
) {
}
