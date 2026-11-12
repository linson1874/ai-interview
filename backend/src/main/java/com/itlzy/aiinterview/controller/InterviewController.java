package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.service.InterviewService;
import com.itlzy.aiinterview.vo.AnswerScoreVO;
import com.itlzy.aiinterview.vo.InterviewCreateVO;
import com.itlzy.aiinterview.vo.InterviewDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 面试场次模块（合同06 §2 三端点 + 合同07 J1 作答评分端点） */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /** 创建面试：POST /interview —— 秒回 interviewId，生成在后台流里跑（06 §2） */
    @PostMapping
    public Result<InterviewCreateVO> create(@RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody CreateInterviewDTO dto) {
        return Result.okData(interviewService.create(userId, dto));
    }

    /** 轮询面：GET /interview/{id} —— status=1 才附题目（R1）；他人/不存在=404（T8） */
    @GetMapping("/{id}")
    public Result<InterviewDetailVO> detail(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long id) {
        return Result.okData(interviewService.detail(userId, id));
    }

    /** 失败返工：POST /interview/{id}/retry（H5：3→0 返工边 + 重投同场；双击第二下 code=1 防抖） */
    @PostMapping("/{id}/retry")
    public Result<Void> retry(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        interviewService.retry(userId, id);
        return Result.ok("ok");
    }

    /** 作答并同步评分：PUT /interview/{id}/answer/{qid}（合同07 J1，单发~10s；
     *  评分失败不阻断提交——code=0 + score=null 的降级边见 Service） */
    @PutMapping("/{id}/answer/{qid}")
    public Result<AnswerScoreVO> answer(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id, @PathVariable Long qid,
                                        @Valid @RequestBody AnswerDTO dto) {
        return Result.okData(interviewService.answer(userId, id, qid, dto));
    }
}