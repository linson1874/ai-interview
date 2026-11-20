package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同09 判分事件（账本-事件层，G1 三表制）：只插入永不改——无 deleted 无 update_time，
 * 表结构层面封死"改历史"的念头。t_skill_profile 的重算之源、v2 算法换代的口粮。
 */
@Data
@TableName("t_review_log")
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属复习单元 */
    private Long reviewItemId;

    /** 判分 0-5（07 评分链原样产出） */
    private Integer score;

    /** B 三档转移结果语义落库：promote(>=4) / demote(<=2) / freeze(=3)——曲线重算不靠从分数反推 */
    private String result;

    /** 判分完成时刻：Java 单时钟，窗口排序键 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
