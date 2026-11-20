package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.ReviewAnswerDTO;
import com.itlzy.aiinterview.service.ReviewService;
import com.itlzy.aiinterview.vo.ReviewAnswerVO;
import com.itlzy.aiinterview.vo.ReviewItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 合同09 C(c) 独立复习面（v1 两端的第一个；/review/answer 在下一步 4b） */
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 今日复习清单：GET /review/today —— 读账本不读投影；归属即消失（无他人可见性问题，
     *  WHERE user_id 就是归属门，01/03/05/06 同款语义第五次复用） */
    @GetMapping("/today")
    public Result<List<ReviewItemVO>> today(@RequestAttribute("userId") Long userId) {
        return Result.okData(reviewService.today(userId));
    }

    /** 提交复习作答：POST /review/answer ——单发 ~10s（判分复用 07 链）；
     *  F2① CAS 防双击、F2② 快照防双跳、失败补偿归 due——三刀细节见 Service */
    @PostMapping("/answer")
    public Result<ReviewAnswerVO> answer(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody ReviewAnswerDTO dto) {
        return Result.okData(reviewService.answer(userId, dto));
    }
}