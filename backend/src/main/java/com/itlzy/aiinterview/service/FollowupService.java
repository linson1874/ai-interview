package com.itlzy.aiinterview.service;

/** 追问业务接口。
 *  @author 林子阳 */
public interface FollowupService {

    /** 发起追问：归属/状态校验 → 幂等查重 → 决策策略 → 题池优先、未命中降级生成 → 落库 */
    com.itlzy.aiinterview.vo.FollowupVO generate(Long userId, Long interviewId, Long questionId);
}