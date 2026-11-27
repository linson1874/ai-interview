package com.itlzy.aiinterview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interviewId;

    private String category;

    private Integer difficulty;

    private String questionContent;

    private String refAnswer;

    /** 追问树父指针；NULL=主题行 */
    private Long parentQuestionId;

    /** 追问策略：下钻/换角度/降档 中文字面 */
    private String strategy;

    /** 追问来源：kb 题池检索命中 / llm 现编；NULL=主题行 */
    private String source;

    /** 来源为题池时的题目 id（溯源用） */
    private Long kbQuestionId;

    private String userAnswer;

    private Integer score;

    private String scoreComment;

    private Integer orderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
