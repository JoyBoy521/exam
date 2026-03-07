package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_notice_read")
public class StudentNoticeRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String noticeId;
    private LocalDateTime readAt;
}
