package com.itlzy.aiinterview.vo;

import lombok.Data;

@Data
public class QuestionItemVO {

    private String category; // 八股|项目|场景|HR
    private Integer difficulty;
    private String question;
}
