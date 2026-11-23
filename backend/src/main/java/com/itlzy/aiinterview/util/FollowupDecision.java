package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.common.FollowupStrategy;

/**
 * 合同10 决策器纯函数（B 定稿）：(父题score, 父题链深) → 策略。
 * 模型不算数也不复述归属——分支判定是本地算术，X1~X4 的证据链从这张表出发。
 * 入参契约：score 非 null（未判分题在端点门就被拒，不进这里）；
 * depth=0 表示父题是主题行，depth=1 表示父题是第一层追问行。
 */
public final class FollowupDecision {

    /** 深度上限=2（B 边4 规则资产，导师裁"不给一行配置的便宜通道"——进代码常量不进 yml） */
    public static final int MAX_DEPTH = 2;

    private FollowupDecision() {
    }

    public static FollowupStrategy decide(int parentScore, int parentDepth) {
        // 边4 优先于一切：到顶必停，无论答什么（X4 的"产品语义"源头）
        if (parentDepth >= MAX_DEPTH) {
            return FollowupStrategy.STOP;
        }
        // 边1：4/5 合并（第二方差区不驱动两种分支，#17 原件）
        if (parentScore >= 4) {
            return FollowupStrategy.DRILL;
        }
        // 边2：3=冻结哲学的追问版，不奖励不惩罚，只多要一个证据
        if (parentScore == 3) {
            return FollowupStrategy.ANGLE;
        }
        // 边3：≤2 全谱降档（0"不会"与 2"明显错误"同边——脚手架都比放弃强）
        return FollowupStrategy.SCAFFOLD;
    }
}