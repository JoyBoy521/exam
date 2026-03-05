package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewMakeupRequest(
        @NotBlank String status,
        String teacherComment,
        Integer approvedExtraMinutes
) {
}
