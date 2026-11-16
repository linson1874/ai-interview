package com.itlzy.aiinterview.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §4.1 注一截断的纯函数测试（converter 式测试债顺势全还）。
 *  住 service.impl 同包才够得着包级 abbreviate——causeContains 同款安排 */
class ReportMaterialTruncateTest {

    @Test
    void shortAnswer_untouched() {
        assertEquals("短答案", AiServiceImpl.abbreviate("短答案", 150));
    }

    @Test
    void boundaryExactly150_notCut() {
        String s = "哈".repeat(150);
        assertEquals(s, AiServiceImpl.abbreviate(s, 150));
    }

    @Test
    void longAnswer_cutTo150WithEllipsis() {
        String r = AiServiceImpl.abbreviate("哈".repeat(200), 150);
        assertEquals(151, r.length());          // 150 字 + 一个省略号字符
        assertTrue(r.endsWith("…"));
        assertEquals("哈".repeat(150) + "…", r);
    }
}
