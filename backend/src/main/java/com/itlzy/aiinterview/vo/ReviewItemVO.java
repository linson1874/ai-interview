package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 合同09 C(c) 独立复习面的今日清单项。materialSource 是诚实标签，前端必须呈现——
 *  A 裁决三级阶梯：kb=题池人工审核答案 / llm=AI参考答案(未经人工审核) / none=裸题干 */
@Data
public class ReviewItemVO {

    /** POST /review/answer 的提交键（复习域自己的 id，不是场次题 id） */
    private Long itemId;

    private String question;

    /** 复习材料：一级=题池答案段，二级=refAnswer，三级=null（只剩题干） */
    private String material;

    /** kb / llm / none——数据与呈现分离，"未经人工审核"的明示由这个字段承载 */
    private String materialSource;

    private Integer boxNo;

    private Integer lastScore;

    private LocalDateTime nextReviewAt;

    /** due / processing——processing 的项照常列出，提交时会被 CAS 弹回明示文案，
     *  比"看不见这题"诚实（W7 复活路径就靠它） */
    private String status;
}