package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 复习判分事件实体：只插入永不修改，是掌握度曲线的重算数据源。
 *  @author 林子阳 */
@Data
@TableName("t_review_log")
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属复习单元 */
    private Long reviewItemId;

    /** 判分 0-5 */
    private Integer score;

    /** 转移结果：promote 前进 / demote 退回 / freeze 冻结 */
    private String result;

    /** 判分完成时刻 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
