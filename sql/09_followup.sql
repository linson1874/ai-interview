-- ============================================================
-- 09_followup.sql —— 追问树四列：父指针/策略/来源/题库引用
-- 引擎: MySQL——追问=题表行的可空增量列，NULL 行全链行为等价
-- 时钟纪律: 本次无新增时间列; update_time 显式 set, 禁 ON UPDATE
-- 兼容论证: MySQL 唯一索引 NULL 不互斥 → 主题行(parent=NULL)不受 uk 约束,
--   出题/作答判分/报告聚合对 NULL 行零变化
-- ============================================================
ALTER TABLE t_interview_question
    ADD COLUMN parent_question_id BIGINT       NULL COMMENT
        '追问树父指针; NULL=主题行。三处执法同词: 总分聚合排除/复习钩子跳过/详情展示过滤',
    ADD COLUMN strategy           VARCHAR(8)   NULL COMMENT
        '追问策略: 下钻/换角度/降档 中文字面，与前端输出零映射',
    ADD COLUMN source             VARCHAR(8)   NULL COMMENT
        '追问来源: kb=题池检索命中 / llm=AI现编; NULL=主题行',
    ADD COLUMN kb_question_id     BIGINT       NULL COMMENT
        'source=kb 时的题池行 id(溯源); llm 行与主题行 NULL',
    ADD UNIQUE KEY uk_parent (parent_question_id) COMMENT
        '一父题至多一个追问孩子=分叉幂等; 重复请求先查后生成, 撞键兜底';