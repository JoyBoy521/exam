package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateRandomPaperRequest {

    @NotBlank(message = "试卷名称不能为空")
    private String title;

    @NotEmpty(message = "抽题规则不能为空")
    private List<RandomRuleItem> rules;

    @Data
    public static class RandomRuleItem {
        private String questionType;  // 题型: SINGLE_CHOICE, TRUE_FALSE, SHORT_ANSWER
        private Integer difficulty;   // 难度: 1(易), 2(中), 3(难)
        private Integer count;        // 抽取数量
        private Integer scorePerItem; // 每题分数
    }
}