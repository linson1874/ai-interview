package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.vo.AnswerScoreVO;
import com.itlzy.aiinterview.vo.InterviewCreateVO;
import com.itlzy.aiinterview.vo.InterviewDetailVO;
import com.itlzy.aiinterview.vo.ReportVO;

public interface InterviewService {

    InterviewCreateVO create(Long userId, CreateInterviewDTO dto);

    /** 轮询面（合同06 §2 GET）：status=1 才附题目 */
    InterviewDetailVO detail(Long userId, Long interviewId);

    /** 失败返工（合同06 H5）：3→0 返工边 + 重投同场 */
    void retry(Long userId, Long id);

    /** 作答并同步评分（合同07 J1）：四步原子——落答案清分→评分→三值回写带 S2 一致性闸；
     *  评分炸穿三层走降级边：答案保留、score=null，不阻断提交 */
    AnswerScoreVO answer(Long userId, Long interviewId, Long questionId, AnswerDTO dto);

    /** 交卷（合同07 J3）：兑现 06 §1 预埋的 1→2 边 + 上报告输送带 */
    void finish(Long userId, Long id);

    /** 查报告（合同07 §2）：归属门→交卷门(P6)→按 report_status 分级；就绪时反序列化 report_json */
    ReportVO getReport(Long userId, Long interviewId);

    /** 手动重生（S4）：CAS 翻回 0 + 重投 interview:report；生成中(1)防抖弹死 */
    void rebuildReport(Long userId, Long interviewId);
}
