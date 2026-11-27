package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.common.FollowupStrategy;

/** 追问策略决策器（纯函数）：根据父题得分与链深决定策略。
 *  depth=0 表示父题是主题行，depth=1 表示父题是第一层追问行。
 *  @author 林子阳 */
public final class FollowupDecision {

    /** 追问深度上限 */
    public static final int MAX_DEPTH = 2;

    private FollowupDecision() {
    }

    public static FollowupStrategy decide(int parentScore, int parentDepth) {
        // 到顶必停，无论答什么
        if (parentDepth >= MAX_DEPTH) {
            return FollowupStrategy.STOP;
        }
        // 高分下钻
        if (parentScore >= 4) {
            return FollowupStrategy.DRILL;
        }
        // 中分换角度验证
        if (parentScore == 3) {
            return FollowupStrategy.ANGLE;
        }
        // 低分降档拆解
        return FollowupStrategy.SCAFFOLD;
    }
}