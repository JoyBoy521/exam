package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateQuestionRequest(
        @NotBlank String type, // 变更为 String
        @NotBlank String stem,
        List<String> options,
        @NotBlank String answer,
        @NotEmpty List<String> knowledgePoints
) {}