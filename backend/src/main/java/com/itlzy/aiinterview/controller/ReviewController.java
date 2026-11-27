package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.ReviewAnswerDTO;
import com.itlzy.aiinterview.service.ReviewService;
import com.itlzy.aiinterview.vo.ReviewAnswerVO;
import com.itlzy.aiinterview.vo.ReviewItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 复习端点。
 *  @author 林子阳 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 今日到期复习清单 */
    @GetMapping("/today")
    public Result<List<ReviewItemVO>> today(@RequestAttribute("userId") Long userId) {
        return Result.okData(reviewService.today(userId));
    }

    /** 提交复习作答并同步判分（约 10 秒） */
    @PostMapping("/answer")
    public Result<ReviewAnswerVO> answer(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody ReviewAnswerDTO dto) {
        return Result.okData(reviewService.answer(userId, dto));
    }
}