package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.itlzy.aiinterview.vo.*;
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
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;   // 和门票存根同款模板，String 序列化=XRANGE 肉眼可读（H1 排障纪律吃到红利）

    public InterviewServiceImpl(InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                ResumeService resumeService,
                                AiService aiService,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.resumeService = resumeService;
        this.aiService = aiService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
            // S13（裁决#30）：降级必须明示——code=0+三null 是静默欺骗客户端。
            // 答案已落库（③步），本方法无事务，抛异常不回滚作答——重交同一答案即重评
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "本题评分未成功，可重新提交重评");
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
            // S13 类推：静默三 null 同样是欺骗。⚠ 文案为技术窗口类推自拟（导师只点了评分失败分支），报备追认
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "已有更新版作答提交，本次评分结果已作废");
        }

        // ⑥ 同步返回三值（P1"三值同列原子落库"的镜像面）
        AnswerScoreVO vo = new AnswerScoreVO();
        vo.setScore(scored.getScore());
        vo.setComment(scored.getComment());
        vo.setRefAnswer(scored.getRefAnswer());
        return vo;
    }

    @Override
    public void finish(Long userId, Long id) {
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, id).eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        // 1→2 原子边（06 §1 预埋今天兑现）；双击第二下 CAS 0 行=防抖，与 retry 同构
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getStatus, 1)
                .set(Interview::getStatus, 2));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "已交卷或场次未完成，请勿重复提交");
        }
        enqueueReport(id);   // 输送带收口：XADD+0→3 补偿见 enqueueReport（S4 注记随方法走）
    }

    @Override
    public ReportVO getReport(Long userId, Long interviewId) {
        Interview iv = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId).eq(Interview::getUserId, userId));
        if (iv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");   // 06 归属三态
        }
        // P6 交卷门：未交卷(状态≠2)拒绝——与作答端点同闸（分母保护，status=2 后作答也被弹死）
        if (iv.getStatus() != 2) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "请先交卷生成报告");
        }
        // 分级响应：0排队/1生成中/3失败都是合法状态——只回 reportStatus，前端据此轮询或引导 rebuild；
        // 2就绪才回全套（report_json 是消费者本地拼好的完整快照，GET 零计算——J1"模型不算数"的存储面）
        if (iv.getReportStatus() != 2 || iv.getReportJson() == null) {
            ReportVO shell = new ReportVO();
            shell.setReportStatus(iv.getReportStatus());
            return shell;
        }
        try {
            ReportVO vo = objectMapper.readValue(iv.getReportJson(), ReportVO.class);
            vo.setReportStatus(2);
            return vo;
        } catch (Exception e) {
            // S9 脏报告永不入库，理论读不炸——防御不吞：真炸了引导用户一键自救
            log.error("report_json 反序列化失败 interviewId={}", interviewId, e);
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "报告数据异常，请点击重新生成");
        }
    }

    @Override
    public void rebuildReport(Long userId, Long interviewId) {
        Interview iv = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId).eq(Interview::getUserId, userId));
        if (iv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        if (iv.getStatus() != 2) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "请先交卷生成报告");
        }
        // S4 防抖（retry 同构）：正在生成(1)时 CAS 0 行弹死。
        // IN(0,2,3) 比 S4 原文(2/3)多一个 0：救"CAS后XADD前崩溃"的孤魂——report 链没有 outbox，
        // rebuild 是唯一救路；重复投不烧双份钱，因消费者抢单 CAS IN(0,1) 天然幂等。⚠ 扩展项，验收时报导师过目
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getReportStatus, 0, 2, 3)
                .set(Interview::getReportStatus, 0));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "报告正在生成中，无需重复操作");
        }
        enqueueReport(interviewId);
    }

    /** 报告输送带（finish 与 rebuild 共用——同一条 XADD+补偿边不许有两个副本）：
     *  XADD 失败→0→3 补偿（3 是 rebuild 合法入口，一键自救，不上发件箱——S4） */
    private void enqueueReport(Long interviewId) {
        try {
            redisTemplate.opsForStream().add(
                    InterviewStream.REPORT_STREAM_KEY,
                    Map.of("interviewId", String.valueOf(interviewId)),
                    RedisStreamCommands.XAddOptions.maxlen(InterviewStream.MAX_LEN)
                            .approximateTrimming(true));
        } catch (Exception e) {
            log.error("报告XADD失败，report_status置3待rebuild interviewId={}", interviewId, e);
            interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getReportStatus, 0)
                    .set(Interview::getReportStatus, 3));
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙，请稍后重试");
        }
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