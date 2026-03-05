package com.exam.system.dto;

public record TeacherExamSummaryResponse(
        Long examId,
        String examTitle,
        long submissionCount,
        double avgTotalScore,
        double highestTotalScore,
        double passRate,
        double manualReviewCompletionRate
) {
}
