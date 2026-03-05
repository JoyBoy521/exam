package com.exam.system.dto;

import java.time.LocalDateTime;

public record TeacherSubmissionViewResponse(
        Long id,
        String studentName,
        double objectiveScore,
        Double manualScore,
        double totalScore,
        String reviewStatus,
        LocalDateTime submittedAt
) {
}
