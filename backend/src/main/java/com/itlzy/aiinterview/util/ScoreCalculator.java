package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.entity.InterviewQuestion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 面试总分计算（纯函数）：按难度加权求均值，保留两位小数。
 *  @author 林子阳 */
public class ScoreCalculator {

    /** 最小已评题数：不足则不出总分 */
    private static final int MIN_SCORED = 3;

    public static BigDecimal computeOverallScore(List<InterviewQuestion> questions) {
        long numerator = 0;    // Σ(score×权重)×100，先乘 100 再除，中间零舍入
        long denominator = 0;  // Σ(5×权重)，未评分的题既不进分子也不进分母
        int scoredCount = 0;

        for (InterviewQuestion q : questions) {
            if (q.getScore() == null) {
                continue;                       // 未评分的题剔除，不计入样本
            }
            int w = q.getDifficulty();          // 权重直接取难度值 1/2/3
            numerator += q.getScore() * w * 100L;
            denominator += 5L * w;
            scoredCount++;
        }

        if (scoredCount < MIN_SCORED) {
            return null;                        // 样本不足返回 null，区别于 0.00 的真考砸
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}