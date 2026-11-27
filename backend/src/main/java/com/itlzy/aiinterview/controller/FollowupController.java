package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.service.FollowupService;
import com.itlzy.aiinterview.vo.FollowupVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 追问端点。
 *  @author 林子阳 */
@RestController
@RequestMapping("/interview")
public class FollowupController {

    private final FollowupService followupService;

    public FollowupController(FollowupService followupService) {
        this.followupService = followupService;
    }

    /** 发起追问：同步生成并返回（约 5 秒），判分仍由作答端点负责 */
    @PostMapping("/{id}/followup/{qid}")
    public Result<FollowupVO> followup(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long id, @PathVariable Long qid) {
        return Result.okData(followupService.generate(userId, id, qid));
    }
}