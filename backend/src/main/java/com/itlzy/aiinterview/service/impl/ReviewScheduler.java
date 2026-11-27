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

/** 复习调度器：定期用数据库账本重建 Redis 到期投影，并探测到期项；判分由用户提交驱动，不在本类。
 *  @author 林子阳 */
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

    /** 启动时按账本全量重建投影 */
    @PostConstruct
    public void init() {
        syncProjection();
    }

    /** 每 5 分钟对账：以数据库账本为准重建 Redis 投影，封顶投影滞后窗口 */
    @Scheduled(fixedDelay = 300_000)
    public void syncProjection() {
        try {
            List<ReviewItem> all = reviewItemMapper.selectList(new LambdaQueryWrapper<ReviewItem>()
                    .isNotNull(ReviewItem::getNextReviewAt));
            // 删后全量重建而非逐条更新：逻辑删除的幽灵成员一并清掉；中途崩溃下轮自愈
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

    /** 每 60 秒探测到期项，仅记日志不删除（领取与判分由作答端点负责） */
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