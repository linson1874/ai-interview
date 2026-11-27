package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

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

    /** 该题的追问树（读时富化，report_json 快照不含此字段） */
    private List<ReportFollowupVO> followups;
}
