package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itlzy.aiinterview.common.InterviewStream;
import com.itlzy.aiinterview.entity.Interview;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.mapper.InterviewMapper;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.util.ScoreCalculator;
import com.itlzy.aiinterview.vo.ReportDetailVO;
import com.itlzy.aiinterview.vo.ReportSchema;
import com.itlzy.aiinterview.vo.ReportVO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 报告消费者：从 Redis Stream 消费报告生成任务，AI 只写定性段，总分由本地计算。
 *  @author 林子阳 */
@Component
public class InterviewReportConsumer {

    private static final Logger log = LoggerFactory.getLogger(InterviewReportConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public InterviewReportConsumer(StringRedisTemplate redisTemplate,
                                   InterviewMapper interviewMapper,
                                   InterviewQuestionMapper questionMapper,
                                   AiService aiService,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        ensureGroup();   // 先建组再认领挂账，顺序不可颠倒
        recoverOwnPel();
    }

    // 以下四个方法与出题消费者同构，仅键名与消费组不同

    /** 创建报告消费组 */
    public void ensureGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    InterviewStream.REPORT_STREAM_KEY, ReadOffset.from("0"), InterviewStream.REPORT_GROUP);
            log.info("报告消费组已创建 group={} stream={}", InterviewStream.REPORT_GROUP, InterviewStream.REPORT_STREAM_KEY);
        } catch (Exception e) {
            if (InterviewGenConsumer.causeContains(e, "BUSYGROUP")) {
                log.info("报告消费组已存在（BUSYGROUP 幂等信号，忽略）");
            } else {
                log.warn("报告消费组创建失败（非 BUSYGROUP），不抛出——运行期由 poll 自愈兜底", e);
            }
        }
    }

    /** 恢复模式：重发自己名下未 ACK 的挂账 */
    public void recoverOwnPel() {
        List<MapRecord<String, Object, Object>> pending = redisTemplate.opsForStream().read(
                Consumer.from(InterviewStream.REPORT_GROUP, InterviewStream.REPORT_CONSUMER),
                StreamReadOptions.empty().count(10),
                StreamOffset.create(InterviewStream.REPORT_STREAM_KEY, ReadOffset.from("0")));
        if (pending != null && !pending.isEmpty()) {
            log.info("报告恢复模式：认领自己名下 {} 条挂账", pending.size());
            pending.forEach(this::process);
        }
    }

    /** 每秒轮询取一条消息消费；消费组丢失时自愈重建 */
    @Scheduled(fixedDelay = 1000)
    public void poll() {
        try {
            List<MapRecord<String, Object, Object>> msgs = redisTemplate.opsForStream().read(
                    Consumer.from(InterviewStream.REPORT_GROUP, InterviewStream.REPORT_CONSUMER),
                    StreamReadOptions.empty().count(1),
                    StreamOffset.create(InterviewStream.REPORT_STREAM_KEY, ReadOffset.lastConsumed()));
            if (msgs != null) {
                msgs.forEach(this::process);
            }
        } catch (Exception e) {
            if (InterviewGenConsumer.causeContains(e, "NOGROUP")) {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(InterviewStream.REPORT_STREAM_KEY))) {
                    log.warn("报告消费组丢失（key存在但组没了），自愈重建");
                    ensureGroup();
                }
            } else {
                log.error("报告轮询异常", e);
            }
        }
    }

    /** 每 5 分钟把闲置超过 600 秒的挂账夺回重投 */
    @Scheduled(fixedDelay = 300_000)
    public void sweep() {
        PendingMessages all = redisTemplate.opsForStream()
                .pending(InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, Range.unbounded(), 100);
        if (all.isEmpty()) {
            return;
        }
        RecordId[] ids = new RecordId[all.size()];
        for (int i = 0; i < all.size(); i++) {
            ids[i] = all.get(i).getId();
        }
        List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream()
                .claim(InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP,
                        InterviewStream.REPORT_CONSUMER, java.time.Duration.ofSeconds(600), ids);
        if (!claimed.isEmpty()) {
            log.info("报告清扫夺回 {} 条僵尸消息", claimed.size());
            claimed.forEach(this::process);
        }
    }

    /** 处理单条消息：毒消息判死 → 抢单 → 查题 → AI 定性段 → 本地计算总分 → 落库 → ACK */
    private void process(MapRecord<String, Object, Object> msg) {
        Long interviewId = Long.valueOf(String.valueOf(msg.getValue().get("interviewId")));

        // 投递次数≥3 判为毒消息：置为报告失败态并 ACK 结案
        PendingMessages detail = redisTemplate.opsForStream()
                .pending(InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP,
                        Range.just(msg.getId().toString()), 1);
        long deliveries = detail.isEmpty() ? 1 : detail.get(0).getTotalDeliveryCount();
        if (deliveries >= 3) {
            int dead = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getReportStatus, 1)
                    .set(Interview::getReportStatus, 3));
            if (dead == 1) {
                log.error("报告毒消息处死 interviewId={} deliveryCount={} 消息ID={}",
                        interviewId, deliveries, msg.getId().toString());
            } else {
                log.warn("报告毒消息销案：行状态非1未落死信，仍ACK结案 interviewId={}", interviewId);
            }
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, msg.getId());
            return;
        }

        // 抢单：仅 0（待生成）/1（接管重投）可抢为 1
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getReportStatus, 0, 1)
                .set(Interview::getReportStatus, 1));
        if (won == 0) {   // 已就绪或已失败，ACK 结案
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, msg.getId());
            return;
        }

        try {
            // 查询题目作为材料源，重新生成时自动使用最新分数
            List<InterviewQuestion> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId)
                            .orderByAsc(InterviewQuestion::getOrderNo));
            // 总分计算只用主题行（物理排除追问），追问行仅作为 AI 叙事材料
            List<InterviewQuestion> mainQuestions = questions.stream()
                    .filter(q -> q.getParentQuestionId() == null).toList();

            // AI 生成摘要与薄弱点；失败则不 ACK 留待重投
            ReportSchema schema = aiService.generateReport(questions);

            // 总分与明细由本地计算，不信任模型输出
            ReportVO vo = new ReportVO();
            vo.setSummary(schema.getSummary());
            vo.setWeakPoints(schema.getWeakPoints());
            vo.setOverallScore(ScoreCalculator.computeOverallScore(mainQuestions));   // 分母不含追问行
            vo.setTotalCount(mainQuestions.size());
            vo.setScoredCount((int) mainQuestions.stream().filter(q -> q.getScore() != null).count());  // 已评题数
            List<ReportDetailVO> details = new ArrayList<>();
            for (InterviewQuestion q : mainQuestions) {
                ReportDetailVO d = new ReportDetailVO();
                d.setId(q.getId());
                d.setCategory(q.getCategory());
                d.setDifficulty(q.getDifficulty());
                d.setQuestion(q.getQuestionContent());
                d.setUserAnswer(q.getUserAnswer());
                d.setScore(q.getScore());
                d.setScoreComment(q.getScoreComment());
                details.add(d);
            }
            vo.setDetails(details);

            // 落库：完整快照进 report_json，总分/摘要冗余独立列；CAS 带前置状态，失败不落库
            String json = objectMapper.writeValueAsString(vo);
            int done = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getReportStatus, 1)
                    .set(Interview::getReportJson, json)
                    .set(Interview::getOverallScore, vo.getOverallScore())
                    .set(Interview::getSummary, vo.getSummary())
                    .set(Interview::getReportStatus, 2));
            if (done == 0) {
                log.error("报告回写边丢失（行已非1，黑天鹅：手改库等），不ACK留PEL interviewId={}", interviewId);
                return;
            }

            // 全部完成后 ACK 结案
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, msg.getId());
            log.info("报告完成 interviewId={} 总分={} 已评{}/{}题 delivery={}",
                    interviewId, vo.getOverallScore(), vo.getScoredCount(), vo.getTotalCount(), deliveries);

        } catch (Exception e) {
            // 失败：不 ACK、状态留在 1，等下次投递（投递次数+1，达阈值判死）
            log.error("报告生成失败，消息留PEL待重投 interviewId={}", interviewId, e);
        }
    }
}