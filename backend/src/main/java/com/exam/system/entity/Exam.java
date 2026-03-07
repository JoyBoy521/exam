package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private Long courseId;
    private Long classId;       // 新增：发放目标班级
    private String paperIds;    // 修改：试卷IDs (存比如 "1,2,3" 代表三套平行卷)

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createTime;
}
