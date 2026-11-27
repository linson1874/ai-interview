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

/** 简历端点。
 *  @author 林子阳 */
@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /** 新建简历 */
    @PostMapping
    public Result<Void> create(@RequestAttribute("userId") Long userId,
                               @Valid @RequestBody ResumeSaveDTO dto) {
        resumeService.create(userId, dto);
        return Result.ok("ok");
    }

    /** 修改简历 */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id,
                               @Valid @RequestBody ResumeSaveDTO dto) {
        resumeService.update(userId, id, dto);
        return Result.ok("ok");
    }

    /** 删除简历（幂等，重复删除也返回成功） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        resumeService.delete(userId, id);
        return Result.ok("ok");
    }

    /** 分页查询本人简历 */
    @GetMapping("/page")
    public Result<PageVO<ResumeListItemVO>> page(@RequestAttribute("userId") Long userId,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.okData(resumeService.page(userId, page, size));
    }

    /** 简历详情 */
    @GetMapping("/{id}")
    public Result<ResumeDetailVO> detail(@RequestAttribute("userId") Long userId,
                                         @PathVariable Long id) {
        return Result.okData(resumeService.detail(userId, id));
    }
}