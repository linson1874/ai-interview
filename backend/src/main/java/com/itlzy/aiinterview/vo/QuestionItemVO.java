package com.itlzy.aiinterview.vo;

import lombok.Data;

@Data
public class QuestionItemVO {

    private Long id; // 题目 id，作答端点的引用键
    private String category; // 八股|项目|场景|HR
    private Integer difficulty;
    private String question;
}
