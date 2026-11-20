package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同09 复习单元（账本-状态层，决策点 D）：身份=场次原题行(A)，盒号/到期为 Leitner 状态(B)，
 * status 是 F2① 提交 CAS 的载体。ZSET review:queue 的真相源——投影可死，账本不死(06 家法)。
 */
@Data
@TableName("t_review_item")
public class ReviewItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 身份锚：t_interview_question.id（只读引用，F1：不给 07 域的表加列） */
    private Long interviewQuestionId;

    /** 聚合键：PG kb_question.id，检索映射 top1 且 d<=0.3 才记；NULL=映射失败，只降材料不降身份(A) */
    private Long kbQuestionId;

    /** Leitner 盒号 1-5；>=4前进 / <=2回盒一 / =3冻结(B) */
    private Integer boxNo;

    /** due / processing（F2① CAS 状态机；processing 带租约防死锁，判分回写后归 due） */
    private String status;

    /** 到期时刻：Java 单时钟写入(#15)，ZSET score 的真相源 */
    private LocalDateTime nextReviewAt;

    /** 上次成功回写时刻——F2② 快照比对键（回写 UPDATE 带 WHERE last_review_at=捞取快照） */
    private LocalDateTime lastReviewAt;

    /** 上次判分 0-5；入队时=面试初分，复习回写后=复习分 */
    private Integer lastScore;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
