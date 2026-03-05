package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMakeupRequest(
        @NotNull Long examId,
        @NotBlank String reason
) {
}
