package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** 合同08 V3（免费）：维度锁死——embedding 列是 vector(1024)，塞 3 维坐标必须当场翻脸。
 *  治的病：换了 embedding 模型（维度变了）却静默混存——一半题 1024 维一半 1536 维，
 *  检索距离全成乱码。PG 的 vector 类型是强类型：维度不匹配=物理拒绝，不给沉默腐败的机会。 */
@SpringBootTest
class KbDimensionLockV3Test {

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void wrongDimensionIsRejectedByTypeSystem() {
        DataAccessException e = assertThrows(DataAccessException.class, () ->
                pgJdbcTemplate.update("INSERT INTO kb_question (content, category, difficulty, status, embedding) " +
                                "VALUES (?, '八股', 1, 'draft', ?::vector)",
                        "V3演练：错误维度的坐标", "[0.1,0.2,0.3]"));
        assertTrue(e.getMessage() != null
                        && (String.valueOf(e).contains("dimensions") || containCause(e, "dimensions")),
                "报错信息应含维度不匹配线索，实际=" + e);
    }

    /** 异常链取证老纪律（#21 causeContains 同款）：包装层里找真凶 */
    private static boolean containCause(Throwable t, String kw) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c.getMessage() != null && c.getMessage().contains(kw)) return true;
        }
        return false;
    }
}