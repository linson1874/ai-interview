package com.itlzy.aiinterview;

import com.itlzy.aiinterview.dao.KnowledgeSearchDao;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 检索质量基线测试（付费，手动跑）：书面 5 + 口语变体 5 的黄金问题集，命中条目必须带答案段。
 *  现行基线写入 docs/eval/v4-baseline.txt，调整阈值前先快照存档，便于 diff 对比。 */
@SpringBootTest
class KbSearchGoldenV4Test {

    /** {组别, 标准问题, 期望命中的题干关键词}，口语组故意缩略/语气词/不完整句 */
    private static final String[][] GOLDENS = {
            {"书面", "MySQL 索引为什么用 B+ 树", "B+"},
            {"书面", "HashMap 的底层原理", "HashMap"},
            {"书面", "Redis 持久化怎么选", "RDB"},
            {"书面", "怎么用 Redis 做分布式锁", "分布式锁"},
            {"书面", "线上接口偶发超时怎么排查", "超时"},
            {"口语", "HashMap咋扩容的啊", "HashMap"},
            {"口语", "索引为啥是B+树不是哈希", "B+"},
            {"口语", "redis持久化选哪个好", "RDB"},
            {"口语", "分布式锁咋搞", "分布式锁"},
            {"口语", "接口偶尔抽风变慢咋办", "超时"},
    };

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private KnowledgeSearchDao searchDao;

    /** 报告头里的阈值读真配置——防止改了 yml 忘改注释，报告自报当前档位 */
    @Value("${kb.search.distance-max}")
    private double distanceMax;

    @Test
    void goldenQueriesBaseline() throws IOException {
        StringBuilder report = new StringBuilder(
                "# V4 golden queries 检索基线 " + LocalDate.now() + "（书面5+口语5 #41）\n"
                        + "# 配置: text-embedding-v4 / topK=3 / distance-max=" + distanceMax + " / 题库=seed-v1(12 approved 含answer_text)\n");
        Map<String, int[]> groupStat = new LinkedHashMap<>();   // 组别 -> {hit, total}
        int hit = 0;
        int emptyAnswerHits = 0;
        for (String[] g : GOLDENS) {
            int[] stat = groupStat.computeIfAbsent(g[0], k -> new int[2]);
            stat[1]++;
            float[] vec = embeddingModel.embed(g[1]);           // 真查询→真向量
            List<KnowledgeSearchDao.KbItem> items = searchDao.search(toLiteral(vec), null);
            boolean matched = items.stream().anyMatch(it -> it.getContent().contains(g[2]));
            if (matched) { hit++; stat[0]++; }
            report.append(String.format("%n[%s] Q: %s（期望含\"%s\"）→ %s%n", g[0], g[1], g[2], matched ? "HIT" : "MISS"));
            if (items.isEmpty()) {
                report.append("   （空手而归：topK 全被 distance-max 拦下）\n");
            }
            for (KnowledgeSearchDao.KbItem it : items) {
                report.append(String.format("   d=%.4f  %s%n", it.getDistance(), it.getContent()));
                // 命中条目无答案段=材料层回退
                if (it.getAnswerText() == null || it.getAnswerText().isBlank()) {
                    emptyAnswerHits++;
                    report.append("   !! 命中条目 id=" + it.getId() + " 无 answer_text\n");
                }
            }
        }
        report.append(String.format("%n=== 总命中率: %d/%d ===%n", hit, GOLDENS.length));
        groupStat.forEach((k, v) -> report.append(String.format("   %s组: %d/%d%n", k, v[0], v[1])));
        System.out.println(report);

        // 基线留档进仓库（工作目录是 backend，所以 ../docs）
        Path file = Path.of("../docs/eval/v4-baseline.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, report);

        assertTrue(hit >= 1, "10 个 golden 全 MISS = 检索链本身有 bug（非调参问题），先修链再谈基线");
        assertEquals(0, emptyAnswerHits, "存在无 answer_text 的命中条目=材料层回退，先补库再论阈值");
    }

    /** float[] → pgvector 字面量 */
    private static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}