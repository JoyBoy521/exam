package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("paper")
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // 下面这两个是 Controller 需要用到的字段，必须加上！
    private BigDecimal totalScore;
    private String createBy;

    private LocalDateTime createTime;

    // 这个注解表示该字段不对应数据库表中的列，仅用于给前端返回包含题目的 JSON 数据
    @TableField(exist = false)
    private List<Long> questionIds;
}