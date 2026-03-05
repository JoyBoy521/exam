package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;         // 关联的考试场次
    private Long userId;         // 答题学生ID
    private String status;       // 状态: SUBMITTED(已交卷待批阅), GRADED(已批阅)

    private BigDecimal objectiveScore;   // 客观题(机器)得分
    private BigDecimal subjectiveScore;  // 主观题(人工)得分
    private BigDecimal totalScore;       // 总分

    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}