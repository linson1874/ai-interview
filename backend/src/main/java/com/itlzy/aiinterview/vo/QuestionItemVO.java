package com.itlzy.aiinterview.vo;

import lombok.Data;

@Data
public class QuestionItemVO {

    private Long id; // 题目身份证——07 作答端点 /answer/{qid} 的引用键（06§2 形状漏项，工单③验收前补上）
    private String category; // 八股|项目|场景|HR
    private Integer difficulty;
    private String question;
}
