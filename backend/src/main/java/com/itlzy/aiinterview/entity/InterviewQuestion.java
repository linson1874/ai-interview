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

    /**追问树父指针；NULL=主题行。三处执法同词：聚合排除/钩子跳过/detail过滤（sql/09 注释的合同面） */
    private Long parentQuestionId;

    /** 决策器结果持久化（#58 补列）：下钻/换角度/降档 中文三值字面（#60 禁翻译层） */
    private String strategy;

    /** 追问来源（C 降级标注-审计面）：kb / llm；NULL=主题行 */
    private String source;

    /** source=kb 时的题池行 id（溯源）；llm 行与主题行 NULL */
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
