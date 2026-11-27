package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.service.InterviewService;
import com.itlzy.aiinterview.vo.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 面试场次端点：创建/详情/作答/交卷/报告/分页/取消。
 *  @author 林子阳 */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /** 创建面试：秒回场次 id，题目在后台异步生成 */
    @PostMapping
    public Result<InterviewCreateVO> create(@RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody CreateInterviewDTO dto) {
        return Result.okData(interviewService.create(userId, dto));
    }

    /** 查询场次详情：仅题目就绪状态附带题目列表；他人或不存在返回 404 */
    @GetMapping("/{id}")
    public Result<InterviewDetailVO> detail(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long id) {
        return Result.okData(interviewService.detail(userId, id));
    }

    /** 重新生成失败场次的题目 */
    @PostMapping("/{id}/retry")
    public Result<Void> retry(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        interviewService.retry(userId, id);
        return Result.ok("ok");
    }

    /** 提交作答并同步评分（约 10 秒）；评分失败时答案已保留，可重新提交重评 */
    @PutMapping("/{id}/answer/{qid}")
    public Result<AnswerScoreVO> answer(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id, @PathVariable Long qid,
                                        @Valid @RequestBody AnswerDTO dto) {
        return Result.okData(interviewService.answer(userId, id, qid, dto));
    }

    /** 交卷：状态转为已交卷并异步启动报告生成 */
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        interviewService.finish(userId, id);
        return Result.ok("已交卷，报告生成中");
    }

    /** 查询报告：按报告生成状态分级返回，就绪时返回完整报告 */
    @GetMapping("/{id}/report")
    public Result<ReportVO> getReport(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long id) {
        return Result.okData(interviewService.getReport(userId, id));
    }

    /** 手动重新生成报告 */
    @PostMapping("/{id}/report/rebuild")
    public Result<Void> rebuildReport(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        interviewService.rebuildReport(userId, id);
        return Result.ok("报告重新生成中");
    }

    /** 分页查询本人面试记录，可按状态筛选（1进行中/2已交卷/3失败/5已取消） */
    @GetMapping("/page")
    public Result<PageVO<InterviewListItemVO>> page(@RequestAttribute("userId") Long userId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer statusFilter) {
        return Result.okData(interviewService.page(userId, page, size, statusFilter));
    }

    /** 取消未就绪的场次（排队中/生成中），重复取消视为成功 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        interviewService.cancel(userId, id);
        return Result.ok("ok");
    }
}