package com.itlzy.aiinterview.vo;

import lombok.Data;

/**
 * 追问回执（合同10 A/E）。source 不进候选人话术但进响应体供前端审计展示（C 标注裁决的"API 不标"
 * 指不做降级提示文案，字段本身是数据不是文案——若导师认为连字段都该藏，删一行的事，待裁）。
 */
@Data
public class FollowupVO {

    /** 追问行 id——候选人拿它走 PUT /answer/{qid}（A 定稿：答追问复用现行端点） */
    private Long questionId;

    private String question;

    /** 中文三值字面（#60 同一路字符串） */
    private String strategy;

    /** kb=题池命中 / llm=AI 现编；主题行 NULL 不出现在这里 */
    private String source;

    /** 这是第几层追问（1 或 2）——派生值随响应走，不落库（D 反幽灵字段判例） */
    private Integer depth;

    /** llm 路径才有收据；kb 路径 null（没烧 chat 的钱，诚实回显） */
    private UsageVO usage;
}