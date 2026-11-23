package com.itlzy.aiinterview.service;

/** 合同10 追问域门面（ReviewService 同构：独立域独立门面，InterviewService 不膨胀） */
public interface FollowupService {

    /**
     * 发起追问（10 §6 端点步主流程）：归属/状态/判分三门 → 幂等先查 → 熔断计数 →
     * 决策器定策略 → 题池优先(kb)/空手降级(llm) → 校验后落库占 uk。
     * CIA：新 HTTP 面=合同批准正门；06/07 零行为变化（A 定稿）；09 零触碰（联动在下一步）。
     */
    com.itlzy.aiinterview.vo.FollowupVO generate(Long userId, Long interviewId, Long questionId);
}