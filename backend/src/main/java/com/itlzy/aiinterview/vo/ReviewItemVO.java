package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 今日复习清单项。 */
@Data
public class ReviewItemVO {

    /** 复习作答端点的提交键（复习项 id，不是场次题 id） */
    private Long itemId;

    private String question;

    /** 复习材料：题池答案 → 参考答案 → 无（只剩题干）三级降级 */
    private String material;

    /** 材料来源：kb 题库 / llm 参考答案 / none 无材料 */
    private String materialSource;

    private Integer boxNo;

    private Integer lastScore;

    private LocalDateTime nextReviewAt;

    /** 状态：due 到期 / processing 处理中（照常列出，提交时会被拒绝） */
    private String status;
}