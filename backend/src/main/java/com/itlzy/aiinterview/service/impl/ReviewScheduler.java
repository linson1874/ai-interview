package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itlzy.aiinterview.common.ReviewKeys;
import com.itlzy.aiinterview.entity.ReviewItem;
import com.itlzy.aiinterview.mapper.ReviewItemMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 合同09 E 节调度器（v1 形态 = 投影修理工 + 到期雷达，不是判分员）。
 * 1. 单实例 fixedDelay 串行，不加认领层（E 裁决：机制不为未发生的部署形态付费）；
 * 2. 账本是真相源：/review/today 查 MySQL 的 idx_user_due，ZSET 只做全局到期观测——
 *    投影级故障爆炸半径=观测盲区，业务零感（06"队列可死、账本不死"平移）；
 * 3. 判分不在这里：v1 判分由用户提交驱动（/review/answer 端点），本类自动判分=替用户答题，语义越权。
 */
@Slf4j
@Component
public class ReviewScheduler {

    private static final int PEEK_LIMIT = 50;

    private final ReviewItemMapper reviewItemMapper;
    private final StringRedisTemplate redisTemplate;

    public ReviewScheduler(ReviewItemMapper reviewItemMapper, StringRedisTemplate redisTemplate) {
        this.reviewItemMapper = reviewItemMapper;
        this.redisTemplate = redisTemplate;
    }

    /** 启动编排：账本→投影全量重建（06 原型③ NOGROUP 自愈平移）。
     *  收口单一 @PostConstruct：同类多个的执行顺序 JVM 不保证（NOGROUP 血案家法） */
    @PostConstruct
    public void init() {
        syncProjection();
    }

    /** 周期对账（5 分钟，06 sweep 同款节拍）：救钩子"账本成功、ZADD 失败"的漏网行，
     *  把投影滞后账本的窗口封顶在 5 分钟 */
    @Scheduled(fixedDelay = 300_000)
    public void syncProjection() {
        try {
            List<ReviewItem> all = reviewItemMapper.selectList(new LambdaQueryWrapper<ReviewItem>()
                    .isNotNull(ReviewItem::getNextReviewAt));
            // DEL+全量 ZADD 而非逐条 upsert：逻辑删除的幽灵成员一并清掉；n<=300 的 O(n) 不值得省。
            // DEL 与 ZADD 之间崩溃=投影空窗，但投影可死账本不死，下轮对账/重启自愈
            redisTemplate.delete(ReviewKeys.QUEUE_KEY);
            if (all.isEmpty()) {
                log.info("复习投影对账：账本空，队列已清空");
                return;
            }
            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            for (ReviewItem item : all) {
                double epoch = item.getNextReviewAt().atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli() / 1000.0;
                tuples.add(new DefaultTypedTuple<>(String.valueOf(item.getId()), epoch));
            }
            redisTemplate.opsForZSet().add(ReviewKeys.QUEUE_KEY, tuples);
            log.info("复习投影对账完成：{} 条到期项已从账本重建", tuples.size());
        } catch (Exception e) {
            log.error("复习投影对账失败（下轮重试，不影响业务）", e);
        }
    }

    /** 到期雷达（60s）：ZRANGEBYSCORE 0..now 不删除（E 主链第 1 步）。
     *  v1 捞到到期只打日志——判分/回队归 /review/answer 端点；本方法同时是 v2 推送的挂载点 */
    @Scheduled(fixedDelay = 60_000)
    public void peekDue() {
        try {
            double now = System.currentTimeMillis() / 1000.0;
            Set<String> due = redisTemplate.opsForZSet().rangeByScore(
                    ReviewKeys.QUEUE_KEY, 0, now, 0, PEEK_LIMIT);
            if (due != null && !due.isEmpty()) {
                log.info("到期雷达：{} 个复习单元已到期（等用户从 /review/today 领取，peek 不删除）", due.size());
            }
        } catch (Exception e) {
            log.error("到期雷达探测失败（下轮重试）", e);
        }
    }
}