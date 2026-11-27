package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itlzy.aiinterview.common.InterviewStream;
import com.itlzy.aiinterview.dto.CreateInterviewDTO;
import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.entity.Interview;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.mapper.InterviewMapper;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.ReviewService;
import com.itlzy.aiinterview.vo.QuestionItemVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 出题消费者：从 Redis Stream 串行消费出题任务，失败自动重投、毒消息判死。
 *  @author 林子阳 */
@Component
public class InterviewGenConsumer {

    private static final Logger log = LoggerFactory.getLogger(InterviewGenConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AiService aiService;
    private final ReviewService reviewService;

    public InterviewGenConsumer(StringRedisTemplate redisTemplate,
                                InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                AiService aiService,
                                ReviewService reviewService) {
        this.redisTemplate = redisTemplate;
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.aiService = aiService;
        this.reviewService = reviewService;
    }

    /** 启动编排：先建消费组，再认领自己名下未 ACK 的挂账（顺序不可颠倒） */
    @PostConstruct
    public void init() {
        ensureGroup();
        recoverOwnPel();
    }

    /** 沿 cause 链查找关键字：Redis 业务错误（BUSYGROUP/NOGROUP 等）藏在根因里，顶层 message 查不到 */
    static boolean causeContains(Throwable e, String token) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (String.valueOf(t.getMessage()).contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** 创建消费组（从头投递）；组已存在则忽略，其余失败仅记日志，运行期由 poll 自愈重建 */
    public void ensureGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    InterviewStream.STREAM_KEY, ReadOffset.from("0"), InterviewStream.GROUP);
            log.info("消费组已创建 group={} stream={}", InterviewStream.GROUP, InterviewStream.STREAM_KEY);
        } catch (Exception e) {
            if (causeContains(e, "BUSYGROUP")) {
                log.info("消费组已存在（BUSYGROUP 幂等信号，忽略）");
            } else {
                log.warn("消费组创建失败（非 BUSYGROUP，启动时 key 可能还不存在），不抛出——运行期由 poll 自愈重建", e);
            }
        }
    }

    /** 恢复模式：接新活前先重发自己名下未 ACK 的挂账（offset="0" 而非 ">"） */
    public void recoverOwnPel() {
        List<MapRecord<String, Object, Object>> pending = redisTemplate.opsForStream().read(
                Consumer.from(InterviewStream.GROUP, InterviewStream.CONSUMER),
                StreamReadOptions.empty().count(10),
                StreamOffset.create(InterviewStream.STREAM_KEY, ReadOffset.from("0")));
        if (pending != null && !pending.isEmpty()) {
            log.info("恢复模式：认领自己名下 {} 条挂账", pending.size());
            pending.forEach(this::process);
        }
    }

    /** 每秒轮询取一条消息消费；消费组丢失时自愈重建，其余异常仅记日志不中断调度 */
    @Scheduled(fixedDelay = 1000)
    public void poll() {
        try {
            List<MapRecord<String, Object, Object>> msgs = redisTemplate.opsForStream().read(
                    Consumer.from(InterviewStream.GROUP, InterviewStream.CONSUMER),
                    StreamReadOptions.empty().count(1),
                    StreamOffset.create(InterviewStream.STREAM_KEY, ReadOffset.lastConsumed()));  // lastConsumed = ">"
            if (msgs != null) {
                msgs.forEach(this::process);
            }
        } catch (Exception e) {
            if (causeContains(e, "NOGROUP")) {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(InterviewStream.STREAM_KEY))) {
                    log.warn("消费组丢失（key存在但组没了），自愈重建");
                    ensureGroup();
                }
                // key 也不存在：无流可消，静默等生产侧把 key 建出来
            } else {
                log.error("轮询异常", e);   // 其他异常只记日志，不中断调度
            }
        }
    }

    /** 处理单条消息：毒消息判死 → 抢单 → 生成题目 → 先删后插 → 置为进行中 → ACK */
    private void process(MapRecord<String, Object, Object> msg) {
        Long interviewId = Long.valueOf(String.valueOf(msg.getValue().get("interviewId")));

        // 投递次数≥3 判为毒消息：置为失败态并 ACK 结案
        PendingMessages detail = redisTemplate.opsForStream()
                .pending(InterviewStream.STREAM_KEY, InterviewStream.GROUP,
                        Range.just(msg.getId().toString()), 1);
        long deliveries = detail.isEmpty() ? 1
                : detail.get(0).getTotalDeliveryCount();
        if (deliveries >= 3) {
            // 仅状态仍为 4 时才置为失败，避免虚报；已被救走/已处理时仍 ACK 结案
            int dead = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 4)
                    .set(Interview::getStatus, 3));
            if (dead == 1) {
                log.error("毒消息处死 interviewId={} deliveryCount={} 消息ID={}",
                        interviewId, deliveries, msg.getId().toString());
            } else {
                log.warn("毒消息销案：行状态非4未落死信，仍ACK结案 interviewId={} 消息ID={}",
                        interviewId, msg.getId().toString());
            }
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());
            return;
        }

        // 抢单：仅 0（排队首抢）/4（接管重投）可抢为 4；单线程串行下安全，扩容前需改为租约式 CAS
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getStatus, 0, 4)
                .set(Interview::getStatus, 4));
        if (won == 0) {   // 已被处理完或场次不存在，ACK 结案
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());
            return;
        }

        try {
            // 读场次取简历与题数
            Interview interview = interviewMapper.selectById(interviewId);

            // 按场次的简历与题数生成题目
            GenerateDTO gen = new GenerateDTO();
            gen.setResumeId(interview.getResumeId());
            gen.setCount(interview.getQuestionCount());
            QuestionsVO questions = aiService.generateQuestions(interview.getUserId(), gen);

            // 删题前先级联清理关联的复习项（题表删除后旧 id 永久失效，顺序不可反）
            reviewService.purgeItemsForInterview(interviewId);

            // 先删后插，保证题目数恒等于场次数不翻倍
            questionMapper.delete(new LambdaQueryWrapper<InterviewQuestion>()
                    .eq(InterviewQuestion::getInterviewId, interviewId));
            int order = 1;
            for (QuestionItemVO item : questions.getQuestions()) {
                InterviewQuestion q = new InterviewQuestion();
                q.setInterviewId(interviewId);
                q.setCategory(item.getCategory());
                q.setDifficulty(item.getDifficulty());
                q.setQuestionContent(item.getQuestion());   // VO 短名映射表的长名
                q.setOrderNo(order++);
                questionMapper.insert(q);
            }

            // 生成完成，置为进行中（可作答）
            interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 4)
                    .set(Interview::getStatus, 1));

            // 全部完成后 ACK 结案
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());
            log.info("出题完成 interviewId={} 题数={} delivery={}", interviewId, order - 1, deliveries);

        } catch (Exception e) {
            // 失败：不 ACK、状态留在 4，等下次投递（投递次数+1，达阈值判死）
            log.error("出题失败，消息留PEL待重投 interviewId={}", interviewId, e);
        }
    }

    /** 每 5 分钟把闲置超过 600 秒的挂账夺回本消费者重投；夺回后投递次数+1，由毒消息阈值判死 */
    @Scheduled(fixedDelay = 300_000)
    public void sweep() {
        PendingMessages all = redisTemplate.opsForStream()
                .pending(InterviewStream.STREAM_KEY, InterviewStream.GROUP, Range.unbounded(), 100);
        if (all.isEmpty()) {
            return;
        }
        RecordId[] ids = new RecordId[all.size()];
        for (int i = 0; i < all.size(); i++) {
            ids[i] = all.get(i).getId();
        }
        List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream()
                .claim(InterviewStream.STREAM_KEY, InterviewStream.GROUP,
                        InterviewStream.CONSUMER, java.time.Duration.ofSeconds(600), ids);
        if (!claimed.isEmpty()) {
            log.info("清扫夺回 {} 条僵尸消息", claimed.size());
            claimed.forEach(this::process);
        }
    }
}