package com.itlzy.aiinterview.dao;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** 题库检索唯一出口：只读审核通过的题目，全项目仅此类可读题库表。
 *  @author 林子阳 */
@Repository
public class KnowledgeSearchDao {

    /** 只查 approved 状态的题目，状态闸焊死在 SQL 常量里 */
    private static final String SELECT_APPROVED =
            "SELECT id, category, difficulty, content, answer_text, tags, embedding <=> ?::vector AS distance " +
                    "FROM kb_question WHERE status = 'approved' ";
    /** 按标签预过滤 */
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

    /** 向量检索：返回距离不超过阈值的最近 topK 条题目；tags 为空时不过滤；向量维度不匹配时数据库直接报错 */
    public List<KbItem> search(String queryVector, List<String> tags) {
        List<Object> args = new ArrayList<>();
        args.add(queryVector);                       // SELECT 中的距离计算参数
        String sql = SELECT_APPROVED;
        if (tags != null && !tags.isEmpty()) {
            sql += TAGS_FILTER;
            args.add("{" + String.join(",", tags) + "}");   // PG 数组字面量 {Java,MySQL}
        }
        sql += ORDER_LIMIT;
        args.add(queryVector);                       // 排序用距离参数
        args.add(topK);
        List<KbItem> rows = pg.query(sql, (rs, i) -> {
            KbItem it = new KbItem();
            it.setId(rs.getLong("id"));
            it.setCategory(rs.getString("category"));
            it.setDifficulty(rs.getInt("difficulty"));
            it.setContent(rs.getString("content"));
            it.setAnswerText(rs.getString("answer_text"));
            // 标签随检索结果带回
            java.sql.Array tagArr = rs.getArray("tags");
            it.setTags(tagArr == null ? List.of() : List.of((String[]) tagArr.getArray()));
            it.setDistance(rs.getDouble("distance"));
            return it;
        }, args.toArray());
        return rows.stream().filter(it -> it.getDistance() <= distanceMax).toList();
    }

    /** 检索结果行 */
    @Data
    public static class KbItem {
        private Long id;
        private String category;
        private Integer difficulty;
        private String content;
        /** 标准答案知识段，旧行可能为 null */
        private String answerText;
        /** 方向轴标签，空数组表示无标签 */
        private List<String> tags;
        private Double distance;
    }

    /** 行映射唯一实现，加列只改一处 */
    private KnowledgeSearchDao.KbItem mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        KbItem it = new KbItem();
        it.setId(rs.getLong("id"));
        it.setCategory(rs.getString("category"));
        it.setDifficulty(rs.getInt("difficulty"));
        it.setContent(rs.getString("content"));
        it.setAnswerText(rs.getString("answer_text"));
        java.sql.Array tagArr = rs.getArray("tags");
        it.setTags(tagArr == null ? List.of() : List.of((String[]) tagArr.getArray()));
        return it;
    }

    /** 按 id 读取题目（同样只读 approved 状态） */
    public KbItem selectById(Long id) {
        List<KbItem> rows = pg.query(
                "SELECT id, category, difficulty, content, answer_text, tags " +
                        "FROM kb_question WHERE id = ? AND status = 'approved'",
                (rs, i) -> {
                    KbItem it = mapRow(rs);
                    it.setDistance(null);
                    return it;
                }, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
}