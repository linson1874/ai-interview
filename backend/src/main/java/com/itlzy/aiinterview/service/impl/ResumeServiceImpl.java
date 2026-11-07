package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.ResumeSaveDTO;
import com.itlzy.aiinterview.entity.Resume;
import com.itlzy.aiinterview.mapper.ResumeMapper;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.vo.PageVO;
import com.itlzy.aiinterview.vo.ResumeDetailVO;
import com.itlzy.aiinterview.vo.ResumeListItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final ObjectMapper objectMapper;   // Spring 工具箱里的现成翻译官，用来skills JSON↔数组

    public ResumeServiceImpl(ResumeMapper resumeMapper, ObjectMapper objectMapper) {
        this.resumeMapper = resumeMapper;
        this.objectMapper = objectMapper;
    }

    /** 合同§2.1+实现备注：skills 先 trim 后判，全空按空数组处理；单条>20字报错。返回清洗后的列表 */
    private List<String> cleanAndValidateSkills(List<String> skills) {
        List<String> cleaned = new ArrayList<>();
        for (String s : skills) {
            if (s == null) continue;
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;               // 空白标签直接丢弃
            if (trimmed.length() > 20) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "单个技能名称不能超过20个字");
            }
            cleaned.add(trimmed);
        }
        if (cleaned.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请至少填写一项技能");
        }
        return cleaned;
    }

    /** 数组→JSON字符串（入库方向）； */
    private String skillsToJson(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (JsonProcessingException e) {
            log.error("skills序列化异常", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙");
        }
    }

    /** JSON字符串→数组（出库方向）；库里值是我们自己写的，读失败=数据被人为改坏，同样兜底 */
    private List<String> jsonToSkills(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("skills反序列化异常, json={}", json, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙");
        }
    }

    /** 合同D1b/D5：按"id+本人"查，查不到=不存在/别人的/已删，三种成因同一个404，不留探测信息 */
    private Resume getOwnResumeOr404(Long userId, Long id) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, id)
                        .eq(Resume::getUserId, userId));   // ← 整个"越权即消失"就靠这行
        if (resume == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        return resume;
    }

    @Override
    public void create(Long userId, ResumeSaveDTO dto) {
        Resume resume = new Resume();
        resume.setUserId(userId);                          // 户主=凭证用户，绝不来自请求参数
        resume.setRealName(dto.getRealName());
        resume.setTargetJob(dto.getTargetJob());
        resume.setEducation(dto.getEducation());
        resume.setSkills(skillsToJson(cleanAndValidateSkills(dto.getSkills())));
        resume.setSelfIntro(dto.getSelfIntro());
        resumeMapper.insert(resume);                       // id/createTime/deleted 都不用手填
    }

    @Override
    public ResumeDetailVO detail(Long userId, Long id) {
        Resume resume = getOwnResumeOr404(userId, id);
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setId(resume.getId());
        vo.setRealName(resume.getRealName());
        vo.setTargetJob(resume.getTargetJob());
        vo.setEducation(resume.getEducation());
        vo.setSkills(jsonToSkills(resume.getSkills()));
        vo.setSelfIntro(resume.getSelfIntro());
        vo.setCreateTime(resume.getCreateTime());
        return vo;
    }

    @Override
    public void update(Long userId, Long id, ResumeSaveDTO dto) {
        Resume resume = getOwnResumeOr404(userId, id);        // 第一步：本人门（404 的三种成因在这一句里统一裁决）

        resume.setRealName(dto.getRealName());
        resume.setTargetJob(dto.getTargetJob());
        resume.setEducation(dto.getEducation());
        resume.setSkills(skillsToJson(cleanAndValidateSkills(dto.getSkills())));
        resume.setSelfIntro(dto.getSelfIntro());
        resumeMapper.updateById(resume);                       // 按主键更新，userId/createTime 等不动
    }

    @Override
    public void delete(Long userId, Long id) {
        resumeMapper.delete(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, id)
                        .eq(Resume::getUserId, userId));
    }

    @Override
    public PageVO<ResumeListItemVO> page(Long userId,long page,long size){
        if(size < 1||size > 50){
            throw new BusinessException(ResultCode.PARAM_INVALID,"每页条数需在1-50之间");
        }
        if(page < 1){
            throw new BusinessException(ResultCode.PARAM_INVALID,"页码不能小于1");
        }

        Page<Resume> mpPage = new Page<>(page,size);
        resumeMapper.selectPage(mpPage,
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId,userId)
                        .orderByDesc(Resume::getCreateTime)
                        .orderByDesc(Resume::getId));
        return PageVO.of(mpPage,this::toListItemVO);
    }

    private ResumeListItemVO toListItemVO(Resume resume){
        ResumeListItemVO vo = new ResumeListItemVO();
        vo.setId(resume.getId());
        vo.setRealName(resume.getRealName());
        vo.setTargetJob(resume.getTargetJob());
        vo.setSkills(jsonToSkills(resume.getSkills()));
        vo.setCreateTime(resume.getCreateTime());
        return vo;
    }

}
