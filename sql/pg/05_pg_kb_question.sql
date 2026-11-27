-- =============================================================
-- sql/pg/05_pg_kb_question.sql — 题池表
-- 引擎: PostgreSQL + pgvector（ai-pgvector 容器, 端口5433, TZ=Asia/Shanghai）
-- 执行: cat sql/pg/05_pg_kb_question.sql | docker exec -i ai-pgvector psql -U postgres -d ai_knowledge
-- =============================================================

CREATE TABLE IF NOT EXISTS kb_question (
    id              bigserial    PRIMARY KEY,
    content         text         NOT NULL,
    category        varchar(8)   NOT NULL,
    difficulty      smallint     NOT NULL,
    tags            text[]       NOT NULL DEFAULT '{}',
    status          varchar(8)   NOT NULL DEFAULT 'draft',
    source          varchar(128),
    embedding       vector(1024),
    embedding_model varchar(64),
    create_time     timestamp    NOT NULL DEFAULT now(),
    update_time     timestamp    NOT NULL DEFAULT now(),
    -- E1 双轴定性：category=题型轴(对齐05合同枚举)，tags=技术方向轴(检索预过滤用这条)
    CONSTRAINT ck_kb_category   CHECK (category   IN ('八股','项目','场景','HR')),
    CONSTRAINT ck_kb_difficulty CHECK (difficulty BETWEEN 1 AND 3),
    CONSTRAINT ck_kb_status     CHECK (status     IN ('draft','pending','approved','retired'))
);

COMMENT ON COLUMN kb_question.embedding       IS 'embedding 模型向量，1024维——换模型/换维度=全表重灌，禁混存';
COMMENT ON COLUMN kb_question.status          IS '审核状态机 draft→pending→approved→retired；运行时检索只读 approved——主闸在检索SQL常量里，不在参数里';
COMMENT ON COLUMN kb_question.tags            IS '技术方向轴（Java/MySQL/Redis...），元数据先行预过滤走这条；category 只管题型配比';

-- 索引阈值：行数<10万 不建向量索引——暴力扫描1024维≈几十ms，
-- 延迟大头是 embedding 的网络往返。到阈值后：
--   CREATE INDEX ON kb_question USING hnsw (embedding vector_cosine_ops);
-- 建完必须用 V4 golden queries 基线回归，证明检索质量无回退才许留着。
-- tags/status/category 同理不建普通索引——规模不到，索引是幻觉；到了一加就有，不是返工。

-- 时钟单源：update_time 由 PG 触发器独家维护，应用侧/脚本侧不许手写。
CREATE OR REPLACE FUNCTION kb_touch_update() RETURNS trigger AS $$
BEGIN
    NEW.update_time := now();
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_kb_touch ON kb_question;
CREATE TRIGGER trg_kb_touch BEFORE UPDATE ON kb_question
    FOR EACH ROW EXECUTE FUNCTION kb_touch_update();

-- 检索查询模板（DAO 唯一出口的实现蓝本）：
--   SELECT id, category, difficulty, content,
--          embedding <=> $1 AS distance          -- 距离=1-相似度
--   FROM kb_question
--   WHERE status = 'approved'                    -- 主闸：代码常量，非参数，调用方改不动
--     AND ($2::text = '' OR category = $2)       -- 题型轴过滤
--     AND (cardinality($3::text[]) = 0 OR tags && $3)  -- 方向轴过滤
--   ORDER BY embedding <=> $1
--   LIMIT $4;                                    -- topK，配置化
-- 阈值(distance-max)在 DAO 出口处过滤。
