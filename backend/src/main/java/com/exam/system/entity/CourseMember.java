package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_members")
public class CourseMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long userId;
    private String memberRole;
    private LocalDateTime joinedAt;
}
