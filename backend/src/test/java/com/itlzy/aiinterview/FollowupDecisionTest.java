package com.itlzy.aiinterview;

import com.itlzy.aiinterview.common.FollowupStrategy;
import com.itlzy.aiinterview.util.FollowupDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 追问决策器手算对拍测试（零 token 零库）。 */
class FollowupDecisionTest {

    @Test
    void 边1_高分下钻含4和5合并() {
        assertEquals(FollowupStrategy.DRILL, FollowupDecision.decide(5, 0));
        assertEquals(FollowupStrategy.DRILL, FollowupDecision.decide(4, 0));
        assertEquals(FollowupStrategy.DRILL, FollowupDecision.decide(4, 1), "第二层发起仍按分数分支");
    }

    @Test
    void 边2_三分换角度() {
        assertEquals(FollowupStrategy.ANGLE, FollowupDecision.decide(3, 0));
    }

    @Test
    void 边3_两分以下全谱降档() {
        for (int s = 0; s <= 2; s++) {
            assertEquals(FollowupStrategy.SCAFFOLD, FollowupDecision.decide(s, 0), "score=" + s);
        }
    }

    @Test
    void 边4_到顶必停无论答什么() {
        for (int s = 0; s <= 5; s++) {
            assertEquals(FollowupStrategy.STOP, FollowupDecision.decide(s, 2), "depth=2 score=" + s);
        }
    }

    @Test
    void STOP无注入字面_调用即程序错误() {
        assertThrows(IllegalStateException.class, () -> FollowupStrategy.STOP.label());
    }
}