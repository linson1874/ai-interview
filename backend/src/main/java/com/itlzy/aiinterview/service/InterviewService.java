package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.vo.*;

/** 面试场次业务接口。
 *  @author 林子阳 */
public interface InterviewService {

    /** 创建面试：秒回场次 id，题目异步生成 */
    InterviewCreateVO create(Long userId, CreateInterviewDTO dto);

    /** 查询场次详情：仅题目就绪状态附带题目列表 */
    InterviewDetailVO detail(Long userId, Long interviewId);

    /** 重试失败场次 */
    void retry(Long userId, Long id);

    /** 提交作答并同步评分；评分失败时答案已保留，可重评 */
    AnswerScoreVO answer(Long userId, Long interviewId, Long questionId, AnswerDTO dto);

    /** 交卷并异步启动报告生成 */
    void finish(Long userId, Long id);

    /** 查询报告：按报告生成状态分级返回 */
    ReportVO getReport(Long userId, Long interviewId);

    /** 手动重新生成报告；生成中防抖 */
    void rebuildReport(Long userId, Long interviewId);

    /** 分页查询本人面试记录，可按状态筛选（1/2/3/5） */
    PageVO<InterviewListItemVO> page(Long userId, long page, long size, Integer statusFilter);

    /** 取消未就绪场次（排队中/生成中），重复取消视为成功 */
    void cancel(Long userId, Long id);
}
