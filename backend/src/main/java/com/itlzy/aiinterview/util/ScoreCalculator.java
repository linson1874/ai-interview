package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.entity.InterviewQuestion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** J4 总分换算——演示口径、单点收口（合同07 §3 J4：五期 CAT 替换的就是这个方法，列契约不变）。
 *  输入直接用实体列表：测试里 new 实体摆字段就行，不另造 DTO */
public class ScoreCalculator {

    /** 最小样本闸：已评<3 不出总分（"两道题的均值不配叫能力分"） */
    private static final int MIN_SCORED = 3;

    public static BigDecimal computeOverallScore(List<InterviewQuestion> questions) {
        long numerator = 0;    // Σ(score×w)×100——先乘 100 再除，中间零舍入（精度收口条款）
        long denominator = 0;  // Σ(5×w)——未评题既不进分子也不进分母（renormalize：没评≠答错）
        int scoredCount = 0;

        for (InterviewQuestion q : questions) {
            if (q.getScore() == null) {
                continue;                       // 条款一：剔除，系统故障不记用户头上
            }
            int w = q.getDifficulty();          // S5 权重直映射 1:2:3，与 difficulty 列同源，零魔法数
            numerator += q.getScore() * w * 100L;
            denominator += 5L * w;
            scoredCount++;
        }

        if (scoredCount < MIN_SCORED) {
            return null;                        // 条款二：样本闸。null=不配出分，区别于 0.00=真考砸
        }
        // 条款三：HALF_UP 两位收口在这发 divide，DECIMAL(5,2) 的家规到 DB 为止
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}