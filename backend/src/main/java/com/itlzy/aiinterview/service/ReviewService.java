package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.entity.InterviewQuestion;

/** 复习业务接口。
 *  @author 林子阳 */
public interface ReviewService {

    /** 错题（score<=2）生成复习单元并写入到期队列；内部异常只记日志不影响主链，同题重复入队由唯一索引保证幂等 */
    void enqueueIfWrong(Long userId, InterviewQuestion question, Integer score);

    /** 今日到期复习清单（真相源为 MySQL，不依赖 Redis 投影） */
    java.util.List<com.itlzy.aiinterview.vo.ReviewItemVO> today(Long userId);

    /** 提交复习作答并判分 */
    com.itlzy.aiinterview.vo.ReviewAnswerVO answer(Long userId,
                                                   com.itlzy.aiinterview.dto.ReviewAnswerDTO dto);

    /** 清理某场面试关联的全部复习项 */
    void purgeItemsForInterview(Long interviewId);
}
