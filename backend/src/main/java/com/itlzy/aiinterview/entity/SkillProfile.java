package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 掌握度曲线实体：一人一轴一键一行，可从判分事件全量重建。
 *  @author 林子阳 */
@Data
@TableName("t_skill_profile")
public class SkillProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 轴类型：tag 方向轴 / category 题型轴 */
    private String axis;

    /** 轴键（不可变快照，源数据变更不回刷历史曲线） */
    private String axisKey;

    /** 掌握度 0-100 = 最近窗口判分均值×20 */
    private BigDecimal mastery;

    /** 最近判分窗口（CSV 左旧右新，跨引擎无法关联重算故自存） */
    private String recentScores;

    /** 累计事件数 */
    private Integer sampleCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
