package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateParallelGroupRequest(
        @NotBlank String name,
        @Size(min = 2, message = "At least two papers are required") List<Long> paperIds
) {
}
