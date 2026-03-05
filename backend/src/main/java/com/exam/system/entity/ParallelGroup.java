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
@TableName(value = "parallel_group", autoResultMap = true)
public class ParallelGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> paperIds;

    private LocalDateTime createTime;
}
