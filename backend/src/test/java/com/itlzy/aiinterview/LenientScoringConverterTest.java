package com.itlzy.aiinterview;

import com.itlzy.aiinterview.util.LenientScoringConverter;
import com.itlzy.aiinterview.vo.ScoringSchema;
import org.junit.jupiter.api.Test;

/** 评分输出清洗器单元测试。 */
class LenientScoringConverterTest {

    private final LenientScoringConverter converter = new LenientScoringConverter();

    @Test
    void 围栏剥净() {
        ScoringSchema s = converter.convert(
                "```json\n{\"score\":4,\"comment\":\"基本正确\",\"refAnswer\":\"应含扩容与树化\"}\n```");
        if (s.getScore() != 4) {
            throw new AssertionError("score 应为 4，实得 " + s.getScore());
        }
    }

    @Test
    void 寒暄混杂截braces() {
        ScoringSchema s = converter.convert(
                "好的面试官，结果如下：{\"score\":3,\"comment\":\"漏细节\",\"refAnswer\":\"完整答案\"} 祝你好运");
        if (s.getScore() != 3) {
            throw new AssertionError("前后寒暄应被 extractBraces 剪掉，实得 score=" + s.getScore());
        }
    }

    @Test
    void 空输入必须炸不许静默() {
        try {
            converter.convert("   ");
            throw new AssertionError("空输出应抛 IllegalArgumentException——L2 不吞错");
        } catch (IllegalArgumentException expected) {
            // 收到预期异常即通过
        }
    }

    @Test
    void 探针一_缺字段会怎样() {
        // 探针用例：无断言，仅打印实测行为
        try {
            ScoringSchema s = converter.convert("{\"score\":5}");
            System.out.println("[探针1] 缺字段=通过 score=" + s.getScore()
                    + " comment=" + s.getComment() + " refAnswer=" + s.getRefAnswer());
        } catch (Exception e) {
            System.out.println("[探针1] 缺字段=抛 " + e.getClass().getSimpleName() + " : " + e.getMessage());
        }
    }

    @Test
    void 探针二_超界分会怎样() {
        // 预测：清洗层只管 JSON 语法，超界分由上层校验拦截
        try {
            ScoringSchema s = converter.convert("{\"score\":9,\"comment\":\"被注入了\",\"refAnswer\":\"x\"}");
            System.out.println("[探针2] 超界=通过 score=" + s.getScore());
        } catch (Exception e) {
            System.out.println("[探针2] 超界=抛 " + e.getClass().getSimpleName());
        }
    }
}