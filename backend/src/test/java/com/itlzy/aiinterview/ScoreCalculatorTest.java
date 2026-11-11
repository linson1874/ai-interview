package com.itlzy.aiinterview;

import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.util.ScoreCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

/** J4 换算器单测（合同07 工单②）——纯函数不起容器，@SpringBootTest 留给真需要 bean 的测试 */
class ScoreCalculatorTest {

    /** 摆一条题目的助记器：只关心 score 和 difficulty 两个字段 */
    private InterviewQuestion q(Integer score, int difficulty) {
        InterviewQuestion question = new InterviewQuestion();
        question.setScore(score);
        question.setDifficulty(difficulty);
        return question;
    }

    /** BigDecimal 断言的公认坑：equals 连 scale 一起比，"66.0" 不等于 "66.00"——
     *  数值相等要用 compareTo==0，这是面试八股，也是本文件所有断言的形状 */
    private void assertAmount(String expected, BigDecimal actual) {
        if (actual == null || new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("期望 " + expected + " 实得 " + actual);
        }
    }

    @Test
    void t1_主公式_难度加权J4讨论表() {
        // Σ(s×w)=5×1+4×2+2×3+3×3+5×1=33 → ×100=3300；Σ(5×w)=5×10=50；3300/50=66.00
        // 对照简单平均 19/25×100=76.00——加权把"难题2分"的痛感如实调低了的活体算术
        assertAmount("66.00", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(4, 2), q(2, 3), q(3, 3), q(5, 1))));
    }

    @Test
    void t2_未评题剔除分母_renormalize() {
        // 拿掉 (2,d3)：Σ(s×w)=27→2700，Σ(5×w)=35，2700/35=77.142857→77.14
        // 条款一验真："没评"不占分母，系统故障不拉低用户总分
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
        // 本文件最重要的一发：0.00=考了且考砸（合法测量值），null=没评（故障）——
        // 两者混同等于把"交白卷"和"阅卷机坏了"印在同一张成绩单上
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
        // (5+4+3)/3=4 → ×20=80.00；公式在 w 恒等时自动还原简单平均——向后兼容的形状证明
        assertAmount("80.00", ScoreCalculator.computeOverallScore(
                List.of(q(5, 1), q(4, 1), q(3, 1))));
    }
}