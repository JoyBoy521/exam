package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateManualPaperRequest(
        @NotBlank String title,
        @NotEmpty List<Long> questionIds
) {
}
