package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parallel_assignment")
public class ParallelAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long studentId;
    private String studentName;
    private Long paperId;
    private LocalDateTime createTime;
}
