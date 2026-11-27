package com.itlzy.aiinterview;

import com.itlzy.aiinterview.dao.KnowledgeSearchDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 审核闸测试：未审核（pending）的题目不得被检索命中；同向量距离为 0 可排除阈值干扰。 */
@SpringBootTest
class KnowledgeSearchV2Test {

    /** 1024 维假向量，与表列定义同维，机制测试不需要真实 embedding */
    private static final String FAKE_VECTOR = "[" + "0.1,".repeat(1023) + "0.1]";

    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private KnowledgeSearchDao searchDao;

    private Long seedId;

    @AfterEach
    void cleanUp() {
        if (seedId != null) {
            pgJdbcTemplate.update("DELETE FROM kb_question WHERE id = ?", seedId);
        }
    }

    @Test
    void pendingNeverHitsApprovedAfter() {
        pgJdbcTemplate.update("INSERT INTO kb_question (content, category, difficulty, tags, status, embedding) " +
                        "VALUES (?, '八股', 1, '{Java}', 'pending', ?::vector)",
                "V2演练：未审核题", FAKE_VECTOR);
        seedId = pgJdbcTemplate.queryForObject("SELECT currval(pg_get_serial_sequence('kb_question','id'))", Long.class);

        List<KnowledgeSearchDao.KbItem> hits = searchDao.search(FAKE_VECTOR, List.of("Java"));
        assertEquals(0, hits.size(), "pending 题被检索命中 = 审核闸失守");

        pgJdbcTemplate.update("UPDATE kb_question SET status = 'approved' WHERE id = ?", seedId);
        hits = searchDao.search(FAKE_VECTOR, List.of("Java"));
        assertEquals(1, hits.size(), "approved 后仍查不到 = 检索链别处断了");
        assertEquals("V2演练：未审核题", hits.get(0).getContent());
        assertEquals(0.0, hits.get(0).getDistance(), 1e-6, "同向量距离应为0");
    }
}