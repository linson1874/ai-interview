package com.itlzy.aiinterview;

import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.util.ScoreCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

/** 总分计算单元测试（纯函数，不起容器）。 */
class ScoreCalculatorTest {

    /** 构造只关心 score/difficulty 两个字的题目 */
    private InterviewQuestion q(Integer score, int difficulty) {
        InterviewQuestion question = new InterviewQuestion();
        question.setScore(score);
        question.setDifficulty(difficulty);
        return question;
    }

    /** BigDecimal 数值相等必须用 compareTo（equals 连精度一起比） */
    private void assertAmount(String expected, BigDecimal actual) {
        if (actual == null || new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("期望 " + expected + " 实得 " + actual);
        }
    }

    @Test
    void t1_主公式_难度加权J4讨论表() {
        // 加权：(5×1+4×2+2×3+3×3+5×1)×100 / (5×10) = 66.00
        assertAmount("66.00", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(4, 2), q(2, 3), q(3, 3), q(5, 1))));
    }

    @Test
    void t2_未评题剔除分母_renormalize() {
        // 未评分的题不进分母：(27×100)/35 = 77.14
        assertAmount("77.14", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(4, 2), q(null, 3), q(3, 3), q(5, 1))));
    }

    @Test
    void t3_最小样本闸_已评两题不配出总分() {
        if (ScoreCalculator.computeOverallScore(
                List.of(q(5, 3), q(4, 2), q(null, 1), q(null, 1), q(null, 2))) != null) {
            throw new AssertionError("已评<3 必须返回 null");
        }
    }

    @Test
    void t4_全零分是0点00不是null() {
        // 0.00=考了且考砸，null=未评分，两者不得混同
        assertAmount("0.00", ScoreCalculator.computeOverallScore(
                List.of(q(0, 1), q(0, 2), q(0, 3))));
    }

    @Test
    void t5_上界全满分() {
        assertAmount("100.00", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(5, 2), q(5, 3))));
    }

    @Test
    void t6_权重退化_全难度1等于简单平均乘20() {
        // 全难度 1 时公式退化为简单平均×20：80.00
        assertAmount("80.00", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(4, 1), q(3, 1))));
    }
}