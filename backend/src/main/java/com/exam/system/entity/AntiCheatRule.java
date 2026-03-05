package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("anti_cheat_rules")
public class AntiCheatRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer versionNo;
    private Integer pageBlurWeight;
    private Integer windowSwitchWeight;
    private Integer networkDisconnectWeight;
    private Integer copyPasteWeight;
    private Integer otherWeight;
    private Integer durationStepSeconds;
    private Integer mediumRiskThreshold;
    private Integer highRiskThreshold;
    private LocalDateTime updatedAt;
}
