package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionsVO {

    private List<QuestionItemVO> questions;
    private String model;
    private UsageVO usage;

}
