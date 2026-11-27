package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 复习单元实体：身份=场次原题，盒号/到期为间隔重复状态；Redis 到期队列的真相源。
 *  @author 林子阳 */
@Data
@TableName("t_review_item")
public class ReviewItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联的面试题目 id（只读引用） */
    private Long interviewQuestionId;

    /** 题库题目 id（检索映射成功才记录，null 表示映射失败只降材料） */
    private Long kbQuestionId;

    /** 间隔重复盒号 1-5 */
    private Integer boxNo;

    /** 状态：due 到期 / processing 处理中（带租约防死锁，回写后归 due） */
    private String status;

    /** 到期时刻，同时作为 Redis 投影的排序分值来源 */
    private LocalDateTime nextReviewAt;

    /** 上次回写时刻，用作并发快照比对键 */
    private LocalDateTime lastReviewAt;

    /** 上次判分 0-5；入队时为面试初分，复习后为复习分 */
    private Integer lastScore;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
