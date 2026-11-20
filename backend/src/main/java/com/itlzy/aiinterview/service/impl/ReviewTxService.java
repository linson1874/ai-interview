package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itlzy.aiinterview.common.ReviewKeys;
import com.itlzy.aiinterview.config.ReviewProperties;
import com.itlzy.aiinterview.entity.ReviewItem;
import com.itlzy.aiinterview.entity.ReviewLog;
import com.itlzy.aiinterview.entity.SkillProfile;
import com.itlzy.aiinterview.mapper.ReviewItemMapper;
import com.itlzy.aiinterview.mapper.ReviewLogMapper;
import com.itlzy.aiinterview.mapper.SkillProfileMapper;
import com.itlzy.aiinterview.util.ProfileWindow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同09 回写事务（G4 的"事务内"落点）：快照比对 → Leitner 转移 → 事件落盘，三步同生共死。
 * 账本先投影后的"账本"段全在这里；投影 ZADD 归调用方在事务提交后做（E）。
 */
@Slf4j
@Service
public class ReviewTxService {

    public record Outcome(String result, int boxNo, LocalDateTime nextReviewAt) {
    }

    private final ReviewItemMapper reviewItemMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final ReviewProperties props;
    private final SkillProfileMapper skillProfileMapper;

    public ReviewTxService(ReviewItemMapper reviewItemMapper,
                           ReviewLogMapper reviewLogMapper,
                           ReviewProperties props,
                           SkillProfileMapper skillProfileMapper) {
        this.reviewItemMapper = reviewItemMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.props = props;
        this.skillProfileMapper = skillProfileMapper;
    }

    /** 返回 null=F2②快照不匹配（判分重演，账本已由更早一写更新），调用方明示丢弃 */
    @Transactional
    public Outcome writeBack(ReviewItem item, int score,String category, java.util.List<String> tags) {
        LocalDateTime now = LocalDateTime.now();   // #15：转移、事件、回写全用这一个 now

        // B 三档：>=4 前进一盒(封顶) / <=2 回盒一 / =3 冻结（方差最大区不驱动状态）
        int box = item.getBoxNo();
        String result;
        int newBox = box;
        if (score >= props.getPromoteMinScore()) {
            result = ReviewKeys.RESULT_PROMOTE;
            newBox = Math.min(props.getIntervalsDays().size(), box + 1);
        } else if (score <= props.getDemoteMaxScore()) {
            result = ReviewKeys.RESULT_DEMOTE;
            newBox = 1;
        } else {
            result = ReviewKeys.RESULT_FREEZE;
        }
        LocalDateTime next = now.plusDays(props.getIntervalsDays().get(newBox - 1));

        LambdaUpdateWrapper<ReviewItem> w = new LambdaUpdateWrapper<ReviewItem>()
                .eq(ReviewItem::getId, item.getId())
                .set(ReviewItem::getBoxNo, newBox)
                .set(ReviewItem::getNextReviewAt, next)
                .set(ReviewItem::getStatus, ReviewKeys.STATUS_DUE)
                .set(ReviewItem::getLastReviewAt, now)
                .set(ReviewItem::getLastScore, score)
                .set(ReviewItem::getUpdateTime, LocalDateTime.now());  // 同 #15 单时钟；fill 在 update(null,wrapper) 形状下不生效，显式补
        // F2② 快照比对——⚠ MP 经典坑：eq(column, null) 生成 "= NULL" 恒 false，
        // 首复习（last_review_at 为 NULL）会被自己的 WHERE 杀死，必须走 isNull 分支
        if (item.getLastReviewAt() == null) {
            w.isNull(ReviewItem::getLastReviewAt);
        } else {
            w.eq(ReviewItem::getLastReviewAt, item.getLastReviewAt());
        }
        int kept = reviewItemMapper.update(null, w);
        if (kept == 0) {
            return null;   // 此刻事务内还没有任何写，直接退出=账本零污染
        }

        ReviewLog row = new ReviewLog();
        row.setReviewItemId(item.getId());
        row.setScore(score);
        row.setResult(result);
        row.setReviewedAt(now);
        reviewLogMapper.insert(row);

        // G4 执法：曲线 upsert 与事件落盘同事务——log 有而行无、行有而 log 无，都不许发生
        upsertProfile(item.getUserId(), ReviewKeys.AXIS_CATEGORY, category, score);
        if (tags != null) {
            for (String tag : tags) {
                upsertProfile(item.getUserId(), ReviewKeys.AXIS_TAG, tag, score);
            }
        }
        return new Outcome(result, newBox, next);
    }

    /** uk(user,axis,axis_key) 一行一轴；读-改-写在事务内，单实例无并发源（E 裁决同款前提） */
    private void upsertProfile(Long userId, String axis, String key, int score) {
        SkillProfile p = skillProfileMapper.selectOne(new LambdaQueryWrapper<SkillProfile>()
                .eq(SkillProfile::getUserId, userId)
                .eq(SkillProfile::getAxis, axis)
                .eq(SkillProfile::getAxisKey, key));
        String csv = ProfileWindow.appendAndTrim(p == null ? null : p.getRecentScores(),
                score, props.getProfileWindow());
        BigDecimal mastery = ProfileWindow.mastery(csv);
        if (p == null) {
            p = new SkillProfile();
            p.setUserId(userId);
            p.setAxis(axis);
            p.setAxisKey(key);
            p.setRecentScores(csv);
            p.setMastery(mastery);
            p.setSampleCount(1);
            skillProfileMapper.insert(p);
        } else {
            p.setRecentScores(csv);
            p.setMastery(mastery);
            p.setSampleCount(p.getSampleCount() + 1);
            skillProfileMapper.updateById(p);
        }
    }
}