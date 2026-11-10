package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.vo.InterviewCreateVO;
import com.itlzy.aiinterview.vo.InterviewDetailVO;

public interface InterviewService {

    InterviewCreateVO create(Long userId, CreateInterviewDTO dto);

    /** 轮询面（合同06 §2 GET）：status=1 才附题目 */
    InterviewDetailVO detail(Long userId, Long interviewId);

    /** 失败返工（合同06 H5）：3→0 返工边 + 重投同场 */
    void retry(Long userId, Long id);
}
