package com.itlzy.aiinterview;

import com.itlzy.aiinterview.service.impl.AiServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 评分输出预算与提示词约束的回归测试：反射读私有常量，不为测试开 public 口子。 */
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
