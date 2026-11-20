package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 复习判分回执：07 三值 + B 转移的可见化——"回盒一"还是"前进到盒2"必须让用户看见 */
@Data
public class ReviewAnswerVO {

    private Integer score;

    private String comment;

    private String refAnswer;

    /** 转移后的盒号（不是转移前！用户关心下一步） */
    private Integer boxNo;

    /** promote / demote / freeze（t_review_log.result 同词汇表） */
    private String result;

    /** 下次到期——"3 天后再见"是复习闭环给用户的唯一承诺 */
    private LocalDateTime nextReviewAt;
}