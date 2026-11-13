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

/** 报告消费者（合同07 J3）：06 四件套复刻卷——ensureGroup/恢复模式/poll自愈/sweep 全同款，
 *  只换命名册三常量（REPORT_*）；处理流程是新的：AI 两段 + Java 本地拼装（golden rule：模型不算数） */
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
        ensureGroup();   // 与 gen 消费者同款编排：先组后账，行序锁死（NOGROUP 血案的家规）
        recoverOwnPel();
    }

    // ── 以下 ensureGroup / recoverOwnPel / poll / sweep 四法与 InterviewGenConsumer 逐行同构，
    //    仅把 STREAM_KEY/GROUP/CONSUMER 换成 REPORT_STREAM_KEY/REPORT_GROUP/REPORT_CONSUMER，
    //    causeContains 借同包工具（第二刀已摘出）。你照 GenConsumer 打开对照着敲，认亲练习 ──

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

    /** 单条消息的一生（报告版）：⓪阈值闸 → ①抢单 → ②查题单 → ③AI两段 → ④本地拼装 → ⑤CAS 1→2 → ACK */
    private void process(MapRecord<String, Object, Object> msg) {
        Long interviewId = Long.valueOf(String.valueOf(msg.getValue().get("interviewId")));

        // ⓪ H4 复刻：delivery≥3 处死 report_status 1→3 + 毒消息也 ACK（死信边同款带前置状态+判行数）
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

        // ① 抢单 CAS IN(0,1)→1 —— R6 智慧平移：0=首抢，1=接管死者现场（rebuild 重投不烧双份钱的凭据）
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getReportStatus, 0, 1)
                .set(Interview::getReportStatus, 1));
        if (won == 0) {   // 已就绪(2)/已处死(3)——活不归我干，ACK 结案（H3 抢输者纪律）
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, msg.getId());
            return;
        }

        try {
            // ② 题单=唯一材料源（H1 读时语义：开工瞬间查库，重生成自动吃最新分数）
            List<InterviewQuestion> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId)
                            .orderByAsc(InterviewQuestion::getOrderNo));

            // ③ AI 只写两个字段（§4.1；L2/L3 再炸原样抛出→失败边：不ACK留PEL，delivery+1 待重投）
            ReportSchema schema = aiService.generateReport(questions);

            // ④ Java 本地拼装（J3 golden rule：模型不算数——算术全在这十行里）
            ReportVO vo = new ReportVO();
            vo.setSummary(schema.getSummary());
            vo.setWeakPoints(schema.getWeakPoints());
            vo.setOverallScore(ScoreCalculator.computeOverallScore(questions));   // J4 三条款在纯函数里已兑现
            vo.setTotalCount(questions.size());
            vo.setScoredCount((int) questions.stream().filter(q -> q.getScore() != null).count());  // J4"已评4题"
            List<ReportDetailVO> details = new ArrayList<>();
            for (InterviewQuestion q : questions) {
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

            // ⑤ 落库：完整快照进 report_json + overall_score/summary 独立列冗余（阶段五 SQL 统计的口子）
            //    CAS 1→2 带前置状态——脏报告走不到这一步（S9：③炸了就抛，压根不 serialize）
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

            // ⑥ 活干完才 ACK（H2 账本销案）
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.REPORT_STREAM_KEY, InterviewStream.REPORT_GROUP, msg.getId());
            log.info("报告完成 interviewId={} 总分={} 已评{}/{}题 delivery={}",
                    interviewId, vo.getOverallScore(), vo.getScoredCount(), vo.getTotalCount(), deliveries);

        } catch (Exception e) {
            // 失败边：不ACK、不回翻——留1等 sweep 夺回重投（三次到阈值见⓪），与 gen 消费者同款哲学
            log.error("报告生成失败，消息留PEL待重投 interviewId={}", interviewId, e);
        }
    }
}