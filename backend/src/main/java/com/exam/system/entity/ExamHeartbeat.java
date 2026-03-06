package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_heartbeats")
public class ExamHeartbeat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private Integer answeredCount;
    private Integer totalCount;
    private Integer timeLeftSeconds;
    private LocalDateTime lastActiveAt;
    private LocalDateTime updatedAt;
}
