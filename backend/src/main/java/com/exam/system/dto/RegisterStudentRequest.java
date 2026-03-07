package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterStudentRequest(
        @NotBlank @Size(min = 4, max = 32) String studentNo,
        @NotBlank @Size(min = 2, max = 32) String name,
        @NotNull Long classId,
        @NotBlank @Size(min = 6, max = 64) String password
) {
}
