package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "question", autoResultMap = true) // autoResultMap 必须加，否则 JSON 无法解析
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String stem;

    // 使用 JacksonTypeHandler 自动将 MySQL 的 JSON 转为 Java 的 List<String>
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    private String answer;

    private String analysis;

    private Integer difficulty;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> knowledgePoints;

    private LocalDateTime createTime;
}