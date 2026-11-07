package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.ResumeSaveDTO;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.vo.PageVO;
import com.itlzy.aiinterview.vo.ResumeDetailVO;
import com.itlzy.aiinterview.vo.ResumeListItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 简历模块（合同03：五个 REST 端点，全部在登录拦截区内，无免检） */
@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /** 新建：POST /resume —— 注意方法里没有任何一处接收 userId 参数，户主从凭证来 */
    @PostMapping
    public Result<Void> create(@RequestAttribute("userId") Long userId,
                               @Valid @RequestBody ResumeSaveDTO dto) {
        resumeService.create(userId, dto);
        return Result.ok("简历保存成功");        // 成功 msg 合同未定稿，暂拟；失败文案才受§2.1约束
    }

    /** 修改：PUT /resume/{id} —— id 在路径上（REST 风格），不在 body */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id,
                               @Valid @RequestBody ResumeSaveDTO dto) {
        resumeService.update(userId, id, dto);
        return Result.ok("简历更新成功");
    }

    /** 删除：DELETE /resume/{id} —— 幂等语义见 Service 的 R6 判决表 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        resumeService.delete(userId, id);
        return Result.ok("简历删除成功");
    }

    /** 分页：GET /resume/page?page=1&size=10 —— defaultValue 兜住"不传"，越界校验在 Service */
    @GetMapping("/page")
    public Result<PageVO<ResumeListItemVO>> page(@RequestAttribute("userId") Long userId,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.okData(resumeService.page(userId, page, size));
    }

    /** 详情：GET /resume/{id} */
    @GetMapping("/{id}")
    public Result<ResumeDetailVO> detail(@RequestAttribute("userId") Long userId,
                                         @PathVariable Long id) {
        return Result.okData(resumeService.detail(userId, id));
    }
}