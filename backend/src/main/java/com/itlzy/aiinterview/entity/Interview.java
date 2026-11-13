package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_interview")
public class Interview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    /** 状态：0-生成中(排队) 4-生成中(已认领) 1-进行中 2-已完成 3-失败 */
    private Integer status;

    private Integer questionCount;

    private BigDecimal overallScore;

    private String summary;

    private Integer reportStatus;

    private String reportJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}