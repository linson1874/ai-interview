package com.itlzy.aiinterview.dao;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同08 K4/K5：题池检索唯一出口（墙二）。
 * 主闸：status='approved' 焊死在 SQL 常量里——方法签名上没有 status 参数，调用方递不进条子；
 * 门框：全项目只有这一个类能读 kb_question（V2 测试看门 + 评审防翻墙，两道各司其职）；
 * 免费可测：SQL 只认数字串，不关心向量是不是百炼真造的——测试用假向量 0 token。
 */
@Repository
public class KnowledgeSearchDao {

    /** topK 截断后、返回前的质量闸（E2：与 <=> 同向，距离<=max 才留） */
    private static final String SELECT_APPROVED =
            "SELECT id, category, difficulty, content, answer_text, embedding <=> ?::vector AS distance " +
                    "FROM kb_question WHERE status = 'approved' ";
    /** tags 轴预过滤（E1 裁决：方向走 tags，category 只管题型四值，两轴不串） */
    private static final String TAGS_FILTER = "AND tags && ?::text[] ";
    private static final String ORDER_LIMIT = "ORDER BY embedding <=> ?::vector LIMIT ?";

    private final JdbcTemplate pg;
    private final int topK;
    private final double distanceMax;

    public KnowledgeSearchDao(JdbcTemplate pgJdbcTemplate,
                              @Value("${kb.search.top-k}") int topK,
                              @Value("${kb.search.distance-max}") double distanceMax) {
        this.pg = pgJdbcTemplate;
        this.topK = topK;
        this.distanceMax = distanceMax;
    }

    /** 一条检索：给定查询向量（1024 维的 "[a,b,...]" 文本形态），返回距离<=max 的最近 topK 条 approved 题。
     *  tags 传 null/空 = 不按方向过滤；查询向量维度≠列定义维度 → PG 直接报错（V3 维度锁的免费副产品） */
    public List<KbItem> search(String queryVector, List<String> tags) {
        List<Object> args = new ArrayList<>();
        args.add(queryVector);                       // SELECT 里的 distance 展示列
        String sql = SELECT_APPROVED;
        if (tags != null && !tags.isEmpty()) {
            sql += TAGS_FILTER;
            args.add("{" + String.join(",", tags) + "}");   // PG 数组字面量 {Java,MySQL}
        }
        sql += ORDER_LIMIT;
        args.add(queryVector);                       // ORDER BY 的距离
        args.add(topK);
        List<KbItem> rows = pg.query(sql, (rs, i) -> {
            KbItem it = new KbItem();
            it.setId(rs.getLong("id"));
            it.setCategory(rs.getString("category"));
            it.setDifficulty(rs.getInt("difficulty"));
            it.setContent(rs.getString("content"));
            it.setAnswerText(rs.getString("answer_text"));
            it.setDistance(rs.getDouble("distance"));
            return it;
        }, args.toArray());
        return rows.stream().filter(it -> it.getDistance() <= distanceMax).toList();
    }

    /** 检索结果行（内部传递件，不出 HTTP 面所以不叫 VO；工单④拼 prompt 和 evidenceKbIds 用它） */
    @Data
    public static class KbItem {
        private Long id;
        private String category;
        private Integer difficulty;
        private String content;
        /** 工单⑤（#36 裁决）：标准答案知识段——ask 链材料主体；老行未补时可能 null */
        private String answerText;
        private Double distance;
    }
}