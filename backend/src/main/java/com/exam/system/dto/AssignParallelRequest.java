package com.exam.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignParallelRequest(
        @NotEmpty List<String> studentNames
) {
}
