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

/** 出题消费者（合同06 工单③）：@Scheduled串行 = H2单线程的框架化实现；启动先清自己PEL再进">" */
@Component
public class InterviewGenConsumer {

    private static final Logger log = LoggerFactory.getLogger(InterviewGenConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AiService aiService;

    public InterviewGenConsumer(StringRedisTemplate redisTemplate,
                                InterviewMapper interviewMapper,
                                InterviewQuestionMapper questionMapper,
                                AiService aiService) {
        this.redisTemplate = redisTemplate;
        this.interviewMapper = interviewMapper;
        this.questionMapper = questionMapper;
        this.aiService = aiService;
    }

    /** 启动编排：先建组、再清自己PEL。两步有严格先后依赖，必须收在同一个 @PostConstruct 里
     *  用代码行序编排——同类多个 @PostConstruct 的执行顺序 JVM 不保证（NOGROUP 血案，2026-11-09） */
    @PostConstruct
    public void init() {
        ensureGroup();
        recoverOwnPel();
    }

    /** 验错误身份证而非外衣：Lettuce 业务错误（BUSYGROUP/NOGROUP）会被 Spring Data 包成
     *  RedisSystemException("Error in execution")，顶层 message 查不到字样——必须沿 cause 链找根因。
     *  实锤两次：11:10 启动真 BUSYGROUP 被误判"非 BUSYGROUP"；昨日刷屏案里 NOGROUP 自愈同款判断从未命中 */
    private static boolean causeContains(Throwable e, String token) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (String.valueOf(t.getMessage()).contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** 建组。from("0")=组从头投递——把之前躺在流里的排队场次全点亮。
     *  v1.2 批注微瑕①修复：catch 只认 BUSYGROUP（真幂等信号），其余异常（如 key 不存在且
     *  不支持 MKSTREAM）不再误归"已存在"——记日志不抛出，运行期由 poll 的 NOGROUP 自愈兑底 */
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

    /** H2 恢复模式：接新活之前先认领自己名下未 ACK 的挂账（由 init 编排，必须在 ensureGroup 之后）。
     *  XREADGROUP 的 offset 用 "0"（不是 ">"）= "把我账上的重发我"——你实验里 consumerA 复活的路径 */
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

    /** 正常消费：每轮取一条（count=1，串行世界观里一次干一件事就够了）。
     *  NOGROUP 自愈：流 key 被 DEL 时消费组随之消失（组是 key 的附属品，两次演练实锤），
     *  调度器对异常只记日志不撤岗→不包扎就每秒刷屏。key 不存在时不建组（无流可消），
     *  等生产侧 XADD 把 key 造出来后，下一轮自愈自然建组——不依赖未验货的 MKSTREAM 行为 */
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
                log.error("轮询异常", e);   // 其他异常留日志，同样不撤岗
            }
        }
    }

    /** 单条消息的一生：阈值闸 → 抢单 → 取数 → AI → 先删后插 → 4→1 → ACK */
    private void process(MapRecord<String, Object, Object> msg) {
        Long interviewId = Long.valueOf(String.valueOf(msg.getValue().get("interviewId")));

        // ── ⓪ H4 阈值闸：delivery≥3 判死刑（动作清单四条全照合同）──
        PendingMessages detail = redisTemplate.opsForStream()
                .pending(InterviewStream.STREAM_KEY, InterviewStream.GROUP,
                        Range.just(msg.getId().toString()), 1);
        long deliveries = detail.isEmpty() ? 1
                : detail.get(0).getTotalDeliveryCount();
        if (deliveries >= 3) {
            // v1.2 批注微瑕②修复：死信边判影响行数——行不在4（已被 retry 救走/已处死）时不虚报"处死"
            int dead = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 4)
                    .set(Interview::getStatus, 3));                        // 死信边（合同原文 WHERE status=4）
            if (dead == 1) {
                log.error("毒消息处死 interviewId={} deliveryCount={} 消息ID={}",
                        interviewId, deliveries, msg.getId().toString());    // 状态进DB，证据进日志
            } else {
                log.warn("毒消息销案：行状态非4未落死信，仍ACK结案 interviewId={} 消息ID={}",
                        interviewId, msg.getId().toString());
            }
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());  // 毒消息也ACK
            return;
        }

        // ── ① 抢单CAS（R6 v1.2 裁决批准：IN(0,4)——0=首抢，4=接管死者现场。三柱：单线程串行/min-idle>最坏处理时长/先删后插收敛。
        //     扩容红线：加线程/加实例前必须先换 update_time 租约式 CAS（合同 §1 写死）──
        int won = interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                .eq(Interview::getId, interviewId)
                .in(Interview::getStatus, 0, 4)
                .set(Interview::getStatus, 4));
        if (won == 0) {   // 活已有人干完/场次不存在 → 我的活到此干完 → ACK结案（H3：抢输必须ACK）
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());
            return;
        }

        try {
            // ── ② 读场次（count 的旅行终点：从这张列里取，不在消息里——H1+03脚本闭环）──
            Interview interview = interviewMapper.selectById(interviewId);

            // ── ③ 复用 Day8 出题漏斗（直调Service，@Max(5)的DTO校验不参与——count=8合法通行）──
            GenerateDTO gen = new GenerateDTO();
            gen.setResumeId(interview.getResumeId());
            gen.setCount(interview.getQuestionCount());
            QuestionsVO questions = aiService.generateQuestions(interview.getUserId(), gen);

            // ── ④ 先删后插（H5：半程遗骸先清，结果恒为N题不翻倍——T4的断言靠它）──
            questionMapper.delete(new LambdaQueryWrapper<InterviewQuestion>()
                    .eq(InterviewQuestion::getInterviewId, interviewId));
            int order = 1;
            for (QuestionItemVO item : questions.getQuestions()) {
                InterviewQuestion q = new InterviewQuestion();
                q.setInterviewId(interviewId);
                q.setCategory(item.getCategory());
                q.setDifficulty(item.getDifficulty());
                q.setQuestionContent(item.getQuestion());   // QuestionItemVO.question → 表的 question_content
                q.setOrderNo(order++);
                questionMapper.insert(q);
            }

            // ── ⑤ 完成边 4→1（题目就绪，用户可作答——R1给1号状态的正身）──
            interviewMapper.update(null, new LambdaUpdateWrapper<Interview>()
                    .eq(Interview::getId, interviewId)
                    .eq(Interview::getStatus, 4)
                    .set(Interview::getStatus, 1));

            // ── ⑥ 全程才ACK：活真正干完了，账本销案 ──
            redisTemplate.opsForStream().acknowledge(
                    InterviewStream.STREAM_KEY, InterviewStream.GROUP, msg.getId());
            log.info("出题完成 interviewId={} 题数={} delivery={}", interviewId, order - 1, deliveries);

        } catch (Exception e) {
            // 失败：不ACK、不回滚状态——留在4，等自恢复/claim再投（delivery+1，三次到阈值见⓪）
            log.error("出题失败，消息留PEL待重投 interviewId={}", interviewId, e);
        }
    }

    /** 工单④清扫（H4 + R8 v1.2 修正：min-idle 300→600s，L3双发×10题×5000尾部≈240s+排队抖动，300s安全垫仅60s）：
     *  每5分钟把"闲置≥600s"的挂账夺到自己名下重投。
     *  两步等价 XAUTOCLAIM：pending 列账拿 ID → claim 按 min-idle 夺（没到 600s 的不返回=不误抢活人）。
     *  夺回的消息 delivery+1，进 process 后由 ⓪阈值闸裁决：三次到顶处死，没到顶重做（抢单边 IN(0,4) 接管） */
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