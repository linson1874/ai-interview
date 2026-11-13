package com.itlzy.aiinterview.vo;

import lombok.Data;

/** 报告逐题明细行（字段=t_interview_question 的题面+作答+判分三值） */
@Data
public class ReportDetailVO {

    private Long id;
    private String category;
    private Integer difficulty;
    private String question;
    private String userAnswer;
    private Integer score;
    private String scoreComment;
}
