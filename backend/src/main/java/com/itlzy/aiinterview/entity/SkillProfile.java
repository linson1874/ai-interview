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

/**
 * 合同09 掌握度曲线（投影层，决策点 D）：一人一轴一键一行(uk)，坏了可从 t_review_log 全量重建。
 * axis 判别列物理隔离两轴(E1"两轴不许混用"的表结构形态)；mastery 是本地算术(W8 golden rule：模型不算数)。
 */
@Data
@TableName("t_skill_profile")
public class SkillProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 轴判别：tag=方向轴(kb_question.tags 快照) / category=题型轴(07 四值) */
    private String axis;

    /** G2 不可变快照：PG 改 tag 不回刷历史曲线——跨引擎幽灵键条款 */
    private String axisKey;

    /** 掌握度 0-100 = 最近 window 次判分均值×20 */
    private BigDecimal mastery;

    /** 08 脚本：窗口自存（CSV 左旧右新）——方向轴跨引擎无法 JOIN 重算，denormalize 换自包含 */
    private String recentScores;

    /** 累计事件数：样本稀的 62% 和样本厚的 62% 不是一回事 */
    private Integer sampleCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
