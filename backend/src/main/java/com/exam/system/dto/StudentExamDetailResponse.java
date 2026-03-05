package com.exam.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentExamDetailResponse(
        Long examId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        List<QuestionItem> questions
) {
    public record QuestionItem(
            Long id,
            String type,
            String stem,
            List<String> options
    ) {}
}
