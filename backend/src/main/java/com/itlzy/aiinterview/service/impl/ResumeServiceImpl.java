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

/** 简历业务逻辑。
 *  @author 林子阳 */
@Slf4j
@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final ObjectMapper objectMapper;   // 用于 skills 的 JSON 序列化与反序列化

    public ResumeServiceImpl(ResumeMapper resumeMapper, ObjectMapper objectMapper) {
        this.resumeMapper = resumeMapper;
        this.objectMapper = objectMapper;
    }

    /** 校验并清洗技能列表：去首尾空白、单条限长 20 字、至少一项 */
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

    /** 技能列表序列化为 JSON 字符串入库 */
    private String skillsToJson(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (JsonProcessingException e) {
            log.error("skills序列化异常", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙");
        }
    }

    /** JSON 字符串反序列化为技能列表；读失败视为数据损坏 */
    private List<String> jsonToSkills(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("skills反序列化异常, json={}", json, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙");
        }
    }

    /** 按 id+本人查询简历，不存在/他人/已删统一 404 */
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
        resume.setUserId(userId);                          // 户主取自凭证用户，不信请求参数
        resume.setRealName(dto.getRealName());
        resume.setTargetJob(dto.getTargetJob());
        resume.setEducation(dto.getEducation());
        resume.setSkills(skillsToJson(cleanAndValidateSkills(dto.getSkills())));
        resume.setSelfIntro(dto.getSelfIntro());
        resumeMapper.insert(resume);
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
        Resume resume = getOwnResumeOr404(userId, id);        // 先过本人门

        resume.setRealName(dto.getRealName());
        resume.setTargetJob(dto.getTargetJob());
        resume.setEducation(dto.getEducation());
        resume.setSkills(skillsToJson(cleanAndValidateSkills(dto.getSkills())));
        resume.setSelfIntro(dto.getSelfIntro());
        resumeMapper.updateById(resume);                       // 按主键更新，其余字段不动
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
