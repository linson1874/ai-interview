package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.entity.InterviewQuestion;

/**
 * 合同09 提分闭环（复习域唯一 Service 门面的第一步：入队）。
 * 调度器（peek+投影）与复习双端点（F2 双防线）在后续步骤并入本门面。
 */
public interface ReviewService {

    /**
     * 07 评分链尾部钩子：score<=demoteMaxScore(2) 的错题生成复习单元并投影进 ZSET。
     * 契约（CIA 定版）：内部全量 try-catch，任何失败只留日志——面试是主链，复习是增值功能，
     * 账本坏了不能赔上面试。幂等由 uk_user_question 撞键保证（同题重评不双入队）。
     */
    void enqueueIfWrong(Long userId, InterviewQuestion question, Integer score);

    /** GET /review/today：读账本（idx_user_due 联合索引就是为这条查询准备的），不读投影——
     *  E 家法：/today 的真相源是 MySQL，ZSET 死了它照响 */
    java.util.List<com.itlzy.aiinterview.vo.ReviewItemVO> today(Long userId);

    /** POST /review/answer：F2 双防线的执法现场（流程注释见 Impl） */
    com.itlzy.aiinterview.vo.ReviewAnswerVO answer(Long userId,
                                                   com.itlzy.aiinterview.dto.ReviewAnswerDTO dto);
}
