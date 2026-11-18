package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 合同08 K4 入库管线（手动触发·付费几分钱）：12 道种子题 → 百炼变向量 → 一律 draft 入库。
 *  纪律三条：
 *  ① 幂等闸：source='seed-v1' 已有存货直接罢工（连点不双灌——07 抢单 CAS 的同款家法）；
 *  ② 审核流：脚本只写 draft，approved 由人工 Navicat 过目后 UPDATE 放行（K4"人工改稿"落地，
 *     工单②的审核闸保证没过目的题检索永远看不见）；
 *  ③ V6 记账：tokens 总数打印在案，回填 K3 成本表（数字进文件不进印象，R2 家法）。 */
@SpringBootTest
class KbIngestScriptTest {

    private static final String SOURCE_TAG = "seed-v1";

    /** {题干, category题型(E1:八股/项目/场景/HR), difficulty(1-3), tags方向轴(逗号分隔)} */
    private static final String[][] SEEDS = {
            {"Java 中 HashMap 的底层实现原理是什么？扩容是如何发生的？", "八股", "2", "Java,Spring"},
            {"== 和 equals() 的区别是什么？重写 equals 为什么必须重写 hashCode？", "八股", "1", "Java"},
            {"简述 JVM 的垃圾回收机制，CMS 和 G1 收集器各有什么特点？", "八股", "3", "Java"},
            {"Java 线程池的核心参数有哪些？拒绝策略如何选型？", "八股", "2", "Java,并发"},
            {"MySQL 为什么选择 B+ 树作为索引结构而不是哈希表或红黑树？", "八股", "2", "MySQL"},
            {"什么是聚簇索引和二级索引？回表是什么，如何避免？", "八股", "2", "MySQL"},
            {"MySQL 事务的隔离级别有哪些？RR 级别如何解决幻读？", "八股", "3", "MySQL"},
            {"Redis 的 RDB 和 AOF 持久化各有什么优劣？生产环境如何选？", "八股", "2", "Redis"},
            {"如何用 Redis 实现分布式锁？有哪些坑？", "场景", "3", "Redis,Java"},
            {"Spring Boot 的自动配置原理是什么？@ConditionalOnMissingBean 起什么作用？", "八股", "2", "Spring,Java"},
            {"你实习/项目里遇到的最有挑战的技术问题是什么？你是如何定位和解决的？", "项目", "2", "行为面"},
            {"如果线上接口偶发超时，你的排查思路是什么？", "场景", "3", "Java,中间件"},
    };

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void ingestSeedQuestions() {
        Integer existed = pgJdbcTemplate.queryForObject(
                "SELECT count(*) FROM kb_question WHERE source = ?", Integer.class, SOURCE_TAG);
        assertNotNull(existed);
        assertTrue(existed == 0, "幂等闸：source=" + SOURCE_TAG + " 已入库 " + existed + " 条，重灌先人工清理");

        List<String> texts = Arrays.stream(SEEDS).map(s -> s[0]).toList();
        // 分批护栏：百炼 embeddings 单请求 batch<=10（实弹 2026-11-18 HTTP 400 "should not be larger than 10"）——
        // OpenAI 兼容协议只保门面不保厂商私规，批量上限这类私规靠自己文档化防御
        int batchSize = 10;
        List<float[]> vectors = new ArrayList<>();
        int tokens = 0;
        for (int from = 0; from < texts.size(); from += batchSize) {
            List<String> batch = texts.subList(from, Math.min(from + batchSize, texts.size()));
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(batch, null));
            assertEquals(batch.size(), response.getResults().size(), "批量返回条数与输入不符=上游行为变了");
            for (Embedding e : response.getResults()) vectors.add(e.getOutput());
            tokens += response.getMetadata().getUsage().getPromptTokens();
        }

        for (int i = 0; i < SEEDS.length; i++) {
            String[] seed = SEEDS[i];
            pgJdbcTemplate.update(
                    "INSERT INTO kb_question (content, category, difficulty, tags, status, source, embedding, embedding_model) " +
                            "VALUES (?, ?, ?, ?::text[], 'draft', ?, ?::vector, ?)",
                    seed[0], seed[1], Integer.parseInt(seed[2]),
                    "{" + seed[3] + "}",
                    SOURCE_TAG, toVectorLiteral(vectors.get(i)), "text-embedding-v4");
        }
        System.out.println("[V6 记账] 入库 " + SEEDS.length + " 题 tokens 合计=" + tokens
                + "（均值=" + tokens / SEEDS.length + "/题）→ 300 题外推≈" + (tokens / SEEDS.length * 300) + " tokens");
        System.out.println("下一步（人工审核仪式）：Navicat 连 5433/ai_knowledge 抽查 content/tags/category 无误后执行：");
        System.out.println("  UPDATE kb_question SET status='approved' WHERE source='" + SOURCE_TAG + "';");
    }

    /** float[] → pgvector 字面量 "[a,b,c...]"——和检索面 ?::vector 同一方言 */
    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}