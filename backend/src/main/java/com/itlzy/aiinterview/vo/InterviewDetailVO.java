package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 场次轮询面（合同06 §2 GET /interview/{id}）：questions 仅 status=1 时非空（R1 题目窗口条款） */
@Data
public class InterviewDetailVO {

    private Long id;
    private Integer status;
    private Integer questionCount;
    private LocalDateTime createTime;

    /** 生成中（0/4）与失败（3）时为 null——前端看 status 决定画进度还是画题 */
    private List<QuestionItemVO> questions;
}
