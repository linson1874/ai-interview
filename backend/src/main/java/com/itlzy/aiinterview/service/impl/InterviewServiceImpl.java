package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.InterviewStream;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.entity.Interview;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.mapper.InterviewMapper;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.service.InterviewService;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.vo.InterviewCreateVO;
import com.itlzy.aiinterview.vo.InterviewDetailVO;
import com.itlzy.aiinterview.vo.QuestionItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final ResumeService resumeService;
    private final StringRedisTemplate redisTemplate;   // 和门票存根同款模板，String 序列化=XRANGE 肉眼可读（H1 排障纪律吃到红利）

    public InterviewServiceImpl(InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                ResumeService resumeService,
                                StringRedisTemplate redisTemplate) {
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.resumeService = resumeService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public InterviewCreateVO create(Long userId, CreateInterviewDTO dto) {
        // 1. 归属门：复用 03 的 detail()——404 门+防枚举语义一并继承（05 同款先例，不许新发明）
        resumeService.detail(userId, dto.getResumeId());

        // 2. 落库 status=0（排队）。IdType.AUTO 的 insert 会把自增 id 回填进实体——下一发 XADD 的内容就是它
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setResumeId(dto.getResumeId());
        interview.setStatus(0);
        interview.setQuestionCount(dto.getCount() == null ? 5 : dto.getCount());   // 缺省主源在此（03脚本注释认领）
        interviewMapper.insert(interview);

        // 3. 上输送带（双写补偿逻辑收口在 enqueueWithCompensation，retry 重投复用同一发）
        enqueueWithCompensation(interview.getId());

        InterviewCreateVO vo = new InterviewCreateVO();
        vo.setInterviewId(interview.getId());
        return vo;
    }

    /** 轮询面（合同06 §2 GET）：归属门 D5 双条件查询；题目仅 status=1 附带（R1 窗口条款） */
    @Override
    public InterviewDetailVO detail(Long userId, Long interviewId) {
        // 归属即消失：查无=404，不区分"不存在/别人的"（03/05 同款语义第三次复用）
        Interview interview = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .eq(Interview::getUserId, userId));
        if (interview == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }

        InterviewDetailVO vo = new InterviewDetailVO();
        vo.setId(interview.getId());
        vo.setStatus(interview.getStatus());
        vo.setQuestionCount(interview.getQuestionCount());
        vo.setCreateTime(interview.getCreateTime());
        // 只有"题目就绪"才附题；0/4 进行中 questions=null，3 失败也 null（前端看 status 分流）
        if (interview.getStatus() == 1) {
            List<QuestionItemVO> items = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId)
                            .orderByAsc(InterviewQuestion::getOrderNo))
                    .stream().map(q -> {
                        QuestionItemVO item = new QuestionItemVO();
                        item.setCategory(q.getCategory());
                        item.setDifficulty(q.getDifficulty());
                        item.setQuestion(q.getQuestionContent());   // 表的长名回 VO 的短名，边界翻译在此
                        return item;
                    }).toList();
            vo.setQuestions(items);
        }
        return vo;
    }

    /** 失败返工（合同06 H5）：归属门 → CAS 3→0 → 重投同场。先翻态后投消息的顺序不可反 */
    @Override
    public void retry(Long userId, Long id) {
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");   // T8：他人场次=404，不泄存在性
        }
        // 只认失败态的 CAS——双击第二下/对进行中场次重试都在这道闸前被弹回（T6）
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getStatus, 3)
                .set(Interview::getStatus, 0));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "生成进行中，无需重试");   // code=1（导师 T6 口径，非 400）
        }
        enqueueWithCompensation(id);   // 重投同一 interviewId，消费者见 IN(0,4) 接活
    }

    /** XADD 带 MAXLEN~（R4）；双写无分布式事务，XADD 失败→就地翻3，不留"永远排队"的孤魂（决策③，F组演练验真） */
    private void enqueueWithCompensation(Long interviewId) {
        try {
            redisTemplate.opsForStream().add(
                    InterviewStream.STREAM_KEY,
                    Map.of("interviewId", String.valueOf(interviewId)),   // H1：线索不是行李，就这一个字段
                    RedisStreamCommands.XAddOptions.maxlen(InterviewStream.MAX_LEN)
                            .approximateTrimming(true));       // = XADD ... MAXLEN ~ 10000，javap 验过货
        } catch (Exception e) {
            log.error("XADD失败，场次回滚为失败态 interviewId={}", interviewId, e);
            // 翻3也走状态机边（WHERE 带前置状态），不是裸 UPDATE
            interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 0)
                    .set(Interview::getStatus, 3));
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙，请稍后重试");
        }
    }
}