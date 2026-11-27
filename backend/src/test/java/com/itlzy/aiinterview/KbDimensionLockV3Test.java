package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** 维度锁测试：向量列维度不匹配时数据库必须拒绝写入，防止混存不同维度的向量。 */
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

    /** 沿 cause 链查找关键字 */
    private static boolean containCause(Throwable t, String kw) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c.getMessage() != null && c.getMessage().contains(kw)) return true;
        }
        return false;
    }
}