package com.itlzy.aiinterview.vo;

import lombok.Data;

/** 评分输出 **/
@Data
public class ScoringSchema {

    private Integer score; //0-5
    private String comment; //中文简评
    private String refAnswer; //参考答案
}
