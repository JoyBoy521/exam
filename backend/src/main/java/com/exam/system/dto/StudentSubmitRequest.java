package com.exam.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record StudentSubmitRequest(
        @NotEmpty Map<Long, String> answers
) {
}
