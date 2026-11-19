package com.itlzy.aiinterview;

import com.itlzy.aiinterview.dao.KnowledgeSearchDao;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 合同08 V4（付费·5 次 embed 约百来 token·手动跑）：检索质量基线——评测意识第一次进回归网。
 *  golden queries：5 个标准问题，各带一个"期望命中的关键词"；topK 命中率写入
 *  docs/eval/v4-baseline.txt 留档——改参数/换模型后重跑对比，就是检索界的 e2e。
 *  断言口径：只要求至少命中 1（全灭=链路 bug）；命中率高低不定及格线——
 *  第一版真实数字是带回给导师定线用的证据（V4 原文只说"记录留档"，没写线）。 */
@SpringBootTest
class KbSearchGoldenV4Test {

    /** {标准问题, 期望在 topK 里看到的题干关键词} */
    private static final String[][] GOLDENS = {
            {"MySQL 索引为什么用 B+ 树", "B+"},
            {"HashMap 的底层原理", "HashMap"},
            {"Redis 持久化怎么选", "RDB"},
            {"怎么用 Redis 做分布式锁", "分布式锁"},
            {"线上接口偶发超时怎么排查", "超时"},
    };

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private KnowledgeSearchDao searchDao;

    @Test
    void goldenQueriesBaseline() throws IOException {
        StringBuilder report = new StringBuilder(
                "# V4 golden queries 检索基线 " + LocalDate.now() + "\n"
                        + "# 配置: text-embedding-v4 / topK=3 / distance-max=0.25 / 题库=seed-v1(12 approved)\n");
        int hit = 0;
        for (String[] g : GOLDENS) {
            float[] vec = embeddingModel.embed(g[0]);          // 真查询→真向量（这发就是查询侧成本）
            List<KnowledgeSearchDao.KbItem> items = searchDao.search(toLiteral(vec), null);
            boolean matched = items.stream().anyMatch(it -> it.getContent().contains(g[1]));
            if (matched) hit++;
            report.append(String.format("%nQ: %s（期望含\"%s\"）→ %s%n", g[0], g[1], matched ? "HIT" : "MISS"));
            if (items.isEmpty()) {
                report.append("   （空手而归：topK 全被 distance-max 拦下）\n");
            }
            for (KnowledgeSearchDao.KbItem it : items) {
                report.append(String.format("   d=%.4f  %s%n", it.getDistance(), it.getContent()));
            }
        }
        report.append(String.format("%n=== 命中率: %d/%d ===%n", hit, GOLDENS.length));
        System.out.println(report);

        // 留档进仓库（工作目录是 backend，所以 ../docs）——数字进文件，R2 家法
        Path file = Path.of("../docs/eval/v4-baseline.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, report);

        assertTrue(hit >= 1, "5 个 golden 全 MISS = 检索链本身有 bug（非调参问题），先修链再谈基线");
    }

    /** float[] → pgvector 字面量（与入库脚本同款小工具，测试间不硬抽公共类——两处重复优于过早抽象） */
    private static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}