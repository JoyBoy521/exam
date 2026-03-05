package com.exam.system.dto;

import java.util.List;
import java.util.Map;

public record StatisticsResponse(
        long questionCount,
        long paperCount,
        long parallelGroupCount,
        long assignmentCount,
        Map<String, Long> questionTypeDistribution,
        List<KnowledgeCoverage> topKnowledgeCoverage,
        Map<String, Long> paperAssignmentDistribution
) {
    public record KnowledgeCoverage(String knowledgePoint, long count) {}
}
