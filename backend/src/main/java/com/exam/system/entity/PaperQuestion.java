package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("paper_question")
public class PaperQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long questionId;

    // 下面这两个是 Controller 需要用到的字段，必须加上！
    private BigDecimal score;
    private Integer sortOrder;
}