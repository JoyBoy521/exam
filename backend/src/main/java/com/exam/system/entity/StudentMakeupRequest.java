package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_makeup_request")
public class StudentMakeupRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private String reason;
    private String status;
    private String teacherComment;
    private Integer approvedExtraMinutes;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
}
