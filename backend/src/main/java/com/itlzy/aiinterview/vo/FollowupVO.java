package com.itlzy.aiinterview.vo;

import lombok.Data;

/** 追问响应。 */
@Data
public class FollowupVO {

    /** 追问题目 id，作答端点的引用键 */
    private Long questionId;

    private String question;

    /** 追问策略中文字面 */
    private String strategy;

    /** 来源：kb 题池命中 / llm 现编 */
    private String source;

    /** 第几层追问（1 或 2），派生值不落库 */
    private Integer depth;

    /** 大模型用量收据；题池命中路径为 null */
    private UsageVO usage;
}