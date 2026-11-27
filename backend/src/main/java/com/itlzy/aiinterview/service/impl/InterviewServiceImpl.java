package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.itlzy.aiinterview.service.ReviewService;
import com.itlzy.aiinterview.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 面试场次业务逻辑：创建/详情/作答/交卷/报告/分页/取消。
 *  @author 林子阳 */
@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final ResumeService resumeService;
    private final AiService aiService;                 // 同步评分入口
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;   // String 序列化，消息体肉眼可读
    private final ReviewService reviewService;         // 复习入队钩子（内部自吞异常，不影响主链）

    public InterviewServiceImpl(InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                ResumeService resumeService,
                                AiService aiService,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                ReviewService reviewService) {
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.resumeService = resumeService;
        this.aiService = aiService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.reviewService = reviewService;
    }

    @Override
    public InterviewCreateVO create(Long userId, CreateInterviewDTO dto) {
        // 校验简历归属，不存在或非本人直接 404
        resumeService.detail(userId, dto.getResumeId());

        // 落库排队态；insert 回填自增 id，供消息体使用
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setResumeId(dto.getResumeId());
        interview.setStatus(0);
        interview.setQuestionCount(dto.getCount() == null ? 5 : dto.getCount());   // 未传时缺省 5 题
        interviewMapper.insert(interview);

        // 投递出题消息
        enqueueWithCompensation(interview.getId());

        InterviewCreateVO vo = new InterviewCreateVO();
        vo.setInterviewId(interview.getId());
        return vo;
    }

    /** 查询场次详情：仅题目就绪状态附带题目列表 */
    @Override
    public InterviewDetailVO detail(Long userId, Long interviewId) {
        // 他人或不存在统一 404，不泄露存在性
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
        // 仅进行中状态附带题目，其余状态前端看 status 分流
        if (interview.getStatus() == 1) {
            List<QuestionItemVO> items = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId)
                            .isNull(InterviewQuestion::getParentQuestionId)
                            .orderByAsc(InterviewQuestion::getOrderNo))
                    .stream().map(q -> {
                        QuestionItemVO item = new QuestionItemVO();
                        item.setId(q.getId());               // 作答端点的引用键
                        item.setCategory(q.getCategory());
                        item.setDifficulty(q.getDifficulty());
                        item.setQuestion(q.getQuestionContent());   // VO 短名映射表的长名
                        return item;
                    }).toList();
            vo.setQuestions(items);
        }
        return vo;
    }

    /** 重试失败场次：先 CAS 置回排队态再重投消息，顺序不可反 */
    @Override
    public void retry(Long userId, Long id) {
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");   // 他人或不存在统一 404
        }
        // 仅失败态可通过 CAS，其余状态直接报错
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getStatus, 3)
                .set(Interview::getStatus, 0));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "生成进行中，无需重试");
        }
        enqueueWithCompensation(id);   // 重投同一场次 id
    }

    /** 提交作答并同步评分 */
    @Override
    public AnswerScoreVO answer(Long userId, Long interviewId, Long questionId, AnswerDTO dto) {
        // 场次归属与状态校验：仅进行中可作答
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        if (own.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "当前场次不可作答");
        }

        // 题目必须属于本场
        InterviewQuestion q = questionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .eq(InterviewQuestion::getInterviewId, interviewId));
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }

        // 先落答案并清空旧分，评分期间库内为"已作答待评分"状态
        questionMapper.update(null, new LambdaUpdateWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .set(InterviewQuestion::getUserAnswer, dto.getAnswer())
                .set(InterviewQuestion::getScore, null));

        // 调用大模型评分（约 10 秒）；失败时答案已保留，可重新提交重评
        ScoringSchema scored;
        try {
            scored = aiService.scoreQuestion(q.getQuestionContent(), q.getDifficulty(), dto.getAnswer());
        } catch (Exception e) {
            log.warn("评分失败，答案已保留待重评 questionId={}", questionId, e);
            // 评分失败显式报错；本方法无事务，抛异常不回滚已落库的答案
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "本题评分未成功，可重新提交重评");
        }

        // 评分回写带一致性闸：仅当答案未被更新时写入；期间用户重交则 0 行命中，丢弃迟到评分
        int fresh = questionMapper.update(null, new LambdaUpdateWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .eq(InterviewQuestion::getUserAnswer, dto.getAnswer())
                .set(InterviewQuestion::getScore, scored.getScore())
                .set(InterviewQuestion::getScoreComment, scored.getComment())
                .set(InterviewQuestion::getRefAnswer, scored.getRefAnswer()));
        if (fresh == 0) {
            log.warn("stale 评分丢弃 questionId={}", questionId);
            // 静默丢弃会欺骗客户端，改为显式报错
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "已有更新版作答提交，本次评分结果已作废");
        }

        // 错题（score<=2）入复习队列；内部自吞异常，不影响主链；仅真落库的分数才触发
        reviewService.enqueueIfWrong(userId, q, scored.getScore());

        // 同步返回评分三值
        AnswerScoreVO vo = new AnswerScoreVO();
        vo.setScore(scored.getScore());
        vo.setComment(scored.getComment());
        vo.setRefAnswer(scored.getRefAnswer());
        return vo;
    }

    /** 分页查询本人面试记录；按创建时间倒序，次键 id 防翻页漂移 */
    @Override
    public PageVO<InterviewListItemVO> page(Long userId, long page, long size, Integer statusFilter) {
        validatePageParams(page, size, statusFilter);
        Page<Interview> mpPage = new Page<>(page, size);
        interviewMapper.selectPage(mpPage, new LambdaQueryWrapper<Interview>()
                .eq(Interview::getUserId, userId)                                    // 只查本人的记录
                .eq(statusFilter != null, Interview::getStatus, statusFilter)
                .orderByDesc(Interview::getCreateTime)
                .orderByDesc(Interview::getId));
        return PageVO.of(mpPage, this::toInterviewListItemVO);
    }

    /** 分页参数校验：越界与非法筛选状态直接报参数错误 */
    static void validatePageParams(long page, long size, Integer statusFilter) {
        if (size < 1 || size > 50) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "每页条数需在1-50之间");
        }
        if (page < 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "页码不能小于1");
        }
        if (statusFilter != null && statusFilter != 1 && statusFilter != 2
                && statusFilter != 3 && statusFilter != 5) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "筛选状态不支持");
        }
    }

    /** 列表行 VO 映射，大字段 summary/report_json 不进列表 */
    private InterviewListItemVO toInterviewListItemVO(Interview iv) {
        InterviewListItemVO vo = new InterviewListItemVO();
        vo.setId(iv.getId());
        vo.setStatus(iv.getStatus());
        vo.setQuestionCount(iv.getQuestionCount());
        vo.setOverallScore(iv.getOverallScore());
        vo.setCreateTime(iv.getCreateTime());
        vo.setResumeId(iv.getResumeId());
        return vo;
    }

    /** 取消未就绪的面试场次（仅排队中/生成中可取消，重复取消视为成功） */
    @Override
    public void cancel(Long userId, Long id) {
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");   // 不区分不存在/他人，避免泄露资源存在性
        }
        // 已取消的场次重复取消视为成功
        if (own.getStatus() == 5) {
            return;
        }
        if (own.getStatus() == 2) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "本场已交卷，无法取消");
        }
        if (own.getStatus() == 1) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "场次进行中，不可取消");
        }
        if (own.getStatus() == 3) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "生成失败场次不可取消");
        }
        // 归属与状态条件在同一条 UPDATE 中校验，将排队中/生成中置为已取消；0 行命中说明状态已被并发迁移
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getUserId, userId)
                .in(Interview::getStatus, 0, 4)
                .set(Interview::getStatus, 5));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "当前状态不可取消");
        }
    }

    /** 交卷：置为已交卷并投递报告生成消息 */
    @Override
    public void finish(Long userId, Long id) {
        Interview own = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, id).eq(Interview::getUserId, userId));
        if (own == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        // 仅进行中可交卷，CAS 防重复提交
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, id)
                .eq(Interview::getStatus, 1)
                .set(Interview::getStatus, 2));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "已交卷或场次未完成，请勿重复提交");
        }
        enqueueReport(id);   // 投递报告生成消息
    }

    /** 查询报告：按报告生成状态分级返回 */
    @Override
    public ReportVO getReport(Long userId, Long interviewId) {
        Interview iv = interviewMapper.selectOne(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getId, interviewId).eq(Interview::getUserId, userId));
        if (iv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }
        // 未交卷不能查报告
        if (iv.getStatus() != 2) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "请先交卷生成报告");
        }
        // 未就绪只回报告状态，前端据此轮询或引导重生成；就绪时返回完整快照
        if (iv.getReportStatus() != 2 || iv.getReportJson() == null) {
            ReportVO shell = new ReportVO();
            shell.setReportStatus(iv.getReportStatus());
            return shell;
        }
        try {
            ReportVO vo = objectMapper.readValue(iv.getReportJson(), ReportVO.class);
            vo.setReportStatus(2);
            attachFollowups(interviewId, vo);   // 读时富化追问树，不改快照
            return vo;
        } catch (Exception e) {
            // 快照损坏时引导用户重新生成
            log.error("report_json 反序列化失败 interviewId={}", interviewId, e);
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "报告数据异常，请点击重新生成");
        }
    }

    /** 报告主题行挂追问树：读时富化，不改动 report_json 快照；无追问为空数组；depth 沿父链派生 */
    private void attachFollowups(Long interviewId, ReportVO vo) {
        if (vo.getDetails() == null || vo.getDetails().isEmpty()) {
            return;
        }
        List<InterviewQuestion> followupRows = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getInterviewId, interviewId)
                .isNotNull(InterviewQuestion::getParentQuestionId)
                .orderByAsc(InterviewQuestion::getId));
        if (followupRows.isEmpty()) {
            vo.getDetails().forEach(d -> d.setFollowups(List.of()));
            return;
        }
        Map<Long, List<InterviewQuestion>> byParent = followupRows.stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getParentQuestionId));
        vo.getDetails().forEach(d -> d.setFollowups(buildFollowupChildren(d.getId(), byParent, 1)));
    }

    /** 递归组装追问树：一父至多一子，叶子返回空数组 */
    static List<ReportFollowupVO> buildFollowupChildren(Long parentId,
                                                         Map<Long, List<InterviewQuestion>> byParent,
                                                         int depth) {
        List<InterviewQuestion> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<ReportFollowupVO> result = new ArrayList<>();
        for (InterviewQuestion q : children) {
            ReportFollowupVO f = new ReportFollowupVO();
            f.setId(q.getId());
            f.setQuestion(q.getQuestionContent());   // VO 短名映射表的长名
            f.setStrategy(q.getStrategy());          // 策略中文字面直接使用库内值
            f.setDepth(depth);
            f.setScore(q.getScore());
            f.setScoreComment(q.getScoreComment());
            f.setUserAnswer(q.getUserAnswer());
            f.setFollowups(buildFollowupChildren(q.getId(), byParent, depth + 1));
            result.add(f);
        }
        return result;
    }

    /** 手动重新生成报告 */
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
        // 生成中(1)防抖；合法前置集里的 0 用于救"CAS 后投递前崩溃"的孤儿状态，重复投递由消费者幂等兜底
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getReportStatus, 0, 2, 3)
                .set(Interview::getReportStatus, 0));
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "报告正在生成中，无需重复操作");
        }
        enqueueReport(interviewId);
    }

    /** 投递报告生成消息（交卷与重新生成共用）；投递失败置为报告失败态，供手动重试 */
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

    /** 补投兜底：每 60 秒扫描排队超过 60 秒未动的场次重投消息；不改状态，重复消息由消费者幂等去重 */
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
                // 单行失败不影响整批：内部已补偿为失败态，继续处理下一行
                log.warn("发件箱补投失败（内部已补偿为失败态）interviewId={}", o.getId());
            }
        }
    }

    /** 投递出题消息；投递失败置为失败态，不留永远排队的孤儿 */
    private void enqueueWithCompensation(Long interviewId) {
        try {
            redisTemplate.opsForStream().add(
                    InterviewStream.STREAM_KEY,
                    Map.of("interviewId", String.valueOf(interviewId)),   // 消息体只带场次 id
                    RedisStreamCommands.XAddOptions.maxlen(InterviewStream.MAX_LEN)
                            .approximateTrimming(true));
        } catch (Exception e) {
            log.error("XADD失败，场次回滚为失败态 interviewId={}", interviewId, e);
            // 带前置状态的定向置为失败
            interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 0)
                    .set(Interview::getStatus, 3));
            throw new BusinessException(ResultCode.SERVER_ERROR, "系统繁忙，请稍后重试");
        }
    }
}