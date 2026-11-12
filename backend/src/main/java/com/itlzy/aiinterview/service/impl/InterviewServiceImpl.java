package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.InterviewStream;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.entity.Interview;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.mapper.InterviewMapper;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.InterviewService;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.vo.AnswerScoreVO;
import com.itlzy.aiinterview.vo.InterviewCreateVO;
import com.itlzy.aiinterview.vo.InterviewDetailVO;
import com.itlzy.aiinterview.vo.QuestionItemVO;
import com.itlzy.aiinterview.vo.ScoringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final ResumeService resumeService;
    private final AiService aiService;            // 合同07 评分链路入口（同步链路直调，不走流——J1 拍板）
    private final StringRedisTemplate redisTemplate;   // 和门票存根同款模板，String 序列化=XRANGE 肉眼可读（H1 排障纪律吃到红利）

    public InterviewServiceImpl(InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                ResumeService resumeService,
                                AiService aiService,
                                StringRedisTemplate redisTemplate) {
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.resumeService = resumeService;
        this.aiService = aiService;
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
                        item.setId(q.getId());               // 07 作答端点的引用键（形状漏项补齐）
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

    /** 作答并同步评分（合同07 J1/J2/S2 的实体化，六步对应工单③卡 e 段） */
    @Override
    public AnswerScoreVO answer(Long userId, Long interviewId, Long questionId, AnswerDTO dto) {
        // ① 场次归属+状态门：非"进行中"不许作答——P5 的 status=2 封卷闸、题目未就绪闸，同一处收口
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        if (own.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "当前场次不可作答");
        }

        // ② 题目归属门：qid 必须属于本场——P3"作答他人题目"的 404 三态统一在此兑现
        InterviewQuestion q = questionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .eq(InterviewQuestion::getInterviewId, interviewId));
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }

        // ③ 先落答案 + 同发清分（S2 语义定稿的实体化）：评分窗口里库内是"已作答待评分"，
        //    绝不出现"新答案旧分数"——这就是 P4 断言的前半段
        questionMapper.update(null, new LambdaUpdateWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .set(InterviewQuestion::getUserAnswer, dto.getAnswer())
                .set(InterviewQuestion::getScore, null));

        // ④ 评分调用（~10s）：炸了走降级边——答案保住、score 留在 null、不阻塞下一题（J1）
        ScoringSchema scored;
        try {
            scored = aiService.scoreQuestion(q.getQuestionContent(), q.getDifficulty(), dto.getAnswer());
        } catch (Exception e) {
            log.warn("评分失败，答案已保留待重评 questionId={}", questionId, e);
            return new AnswerScoreVO();                    // 三字段全 null：提交成功、评分待补（重交同答案=重评）
        }

        // ⑤ 三值回写 + S2 一致性闸：WHERE user_answer=被评原文——这10秒里用户又重交的话，
        //    新答案已覆盖库，这条迟到评分等值不匹配→0行→丢弃（P9 的 stale 分支）
        int fresh = questionMapper.update(null, new LambdaUpdateWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .eq(InterviewQuestion::getUserAnswer, dto.getAnswer())
                .set(InterviewQuestion::getScore, scored.getScore())
                .set(InterviewQuestion::getScoreComment, scored.getComment())
                .set(InterviewQuestion::getRefAnswer, scored.getRefAnswer()));
        if (fresh == 0) {
            log.warn("stale 评分丢弃 questionId={}", questionId);
            return new AnswerScoreVO();                    // 这一发被更新版取代，用户会收到后到那份新分
        }

        // ⑥ 同步返回三值（P1"三值同列原子落库"的镜像面）
        AnswerScoreVO vo = new AnswerScoreVO();
        vo.setScore(scored.getScore());
        vo.setComment(scored.getComment());
        vo.setRefAnswer(scored.getRefAnswer());
        return vo;
    }

    /** R7（v1.2 裁决）事务性发件箱补投：系统动作救"丢失(0不碰)"——每60s扫 status=0 且
     *  update_time 60s 没动的行，复用 enqueueWithCompensation 重上输送带（不改状态、不碰状态机边）。
     *  若消息其实已在飞，消费者抢单CAS+先删后插自然去重。覆盖两类孤魂：insert与XADD之间崩溃、
     *  演练DEL火化消息（活标本 id=1、2：部署后 60s 内自愈即为其验收）。
     *  与 retry 分工：retry=用户动作救"失败(3→0)"；补投=系统动作救"丢失(0)" */
    @Scheduled(fixedDelay = 60_000)
    public void outboxResend() {
        List<Interview> orphans = interviewMapper.selectList(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, 0)
                .lt(Interview::getUpdateTime, LocalDateTime.now().minusSeconds(60)));
        for (Interview o : orphans) {
            log.info("发件箱补投：status=0 且 60s 无动静的孤魂行 re-enqueue interviewId={}", o.getId());
            try {
                enqueueWithCompensation(o.getId());
            } catch (Exception e) {
                // 单行失败不炸整批：enqueueWithCompensation 内部已把该行补偿翻3，这里只需继续救下一魂
                log.warn("发件箱补投失败（内部已补偿为失败态）interviewId={}", o.getId());
            }
        }
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