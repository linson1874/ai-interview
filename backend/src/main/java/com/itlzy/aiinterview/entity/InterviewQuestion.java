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

    private String userAnswer;

    private Integer score;

    private String scoreComment;

    private Integer orderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
