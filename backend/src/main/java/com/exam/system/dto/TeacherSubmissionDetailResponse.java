package com.exam.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherSubmissionDetailResponse(
        Long submissionId,
        Long examId,
        String examTitle,
        String studentName,
        LocalDateTime submittedAt,
        List<AnswerItem> answers
) {
    public record AnswerItem(
            Long questionId,
            String questionType,
            String stem,
            String studentAnswer,
            String referenceAnswer,
            boolean objective,
            boolean answerMatched
    ) {
    }
}
