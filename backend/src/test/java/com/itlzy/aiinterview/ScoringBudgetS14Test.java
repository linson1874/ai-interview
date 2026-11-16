package com.itlzy.aiinterview;

import com.itlzy.aiinterview.service.impl.AiServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S14（裁决#31）：800 护栏腰斩 refAnswer=JsonEOF 病根。断言两件套：预算 1200 + prompt 长度约束句在位。
 *  读 private 常量用反射：不为此开 public 口子——生产 API 面不为测试让步。
 *  故意不加 @SpringBootTest：纯反射零上下文，毫秒级，四测试里最便宜的一发 */
class ScoringBudgetS14Test {

    @Test
    void scoreMaxTokens_shouldBe1200() throws Exception {
        Field f = AiServiceImpl.class.getDeclaredField("SCORE_MAX_TOKENS");
        f.setAccessible(true);
        assertEquals(1200, f.getInt(null));
    }

    @Test
    void scoringPrompt_shouldCapRefAnswerLength() throws Exception {
        Field f = AiServiceImpl.class.getDeclaredField("SCORING_SYSTEM_PROMPT");
        f.setAccessible(true);
        assertTrue(((String) f.get(null)).contains("不超过200字"));
    }
}
