package com.exam.system.dto;

public record TeacherTodoResponse(
        long draftLikeExamCount,
        long ongoingExamCount,
        long finishedExamCount,
        long totalSubmissionCount,
        long pendingManualReviewCount
) {
}
