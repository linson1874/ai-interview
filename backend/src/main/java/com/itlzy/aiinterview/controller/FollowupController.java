package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.service.FollowupService;
import com.itlzy.aiinterview.vo.FollowupVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 合同10 追问端点（A 定稿：独立正门，无请求体——参数全在路径，追问不需要用户填任何东西） */
@RestController
@RequestMapping("/interview")
public class FollowupController {

    private final FollowupService followupService;

    public FollowupController(FollowupService followupService) {
        this.followupService = followupService;
    }

    /** 发起追问：POST /interview/{id}/followup/{qid} —— ~5s 同步（一发生成，E 裁"判分不归本端点"） */
    @PostMapping("/{id}/followup/{qid}")
    public Result<FollowupVO> followup(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long id, @PathVariable Long qid) {
        return Result.okData(followupService.generate(userId, id, qid));
    }
}