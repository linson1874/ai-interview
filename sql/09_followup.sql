-- ============================================================
-- 09_followup.sql —— 合同10 D 数据形状（#58 定稿：四列，含导师补的 strategy）
-- 编号口径(G3 法定数法): 全局第 9 份 / MySQL 第 7 份(01,02,03,04,07,08+本份)
-- 引擎: MySQL——追问=题表行(D 裁决: 可空增量列, NULL 行全链行为等价;
--   F1 判例不否决, 10 契约域内明示升版)
-- 时钟纪律: 本次无新增时间列; update_time 沿 #46 显式 set 先例, 禁 ON UPDATE
-- 兼容论证: MySQL 唯一索引 NULL 不互斥 → 主例题行(parent=NULL)不受 uk 约束,
--   06 消费者先删后插/07 作答判分/报告聚合对 NULL 行零变化
-- ============================================================
ALTER TABLE t_interview_question
    ADD COLUMN parent_question_id BIGINT       NULL COMMENT
        '追问树父指针; NULL=主题行。三处执法同词: overall聚合排除(D硬约束X6)/钩子跳过追问行(B边3-c)/detail展示过滤(D)',
    ADD COLUMN strategy           VARCHAR(8)   NULL COMMENT
        '决策器结果持久化(#58补列): 下钻/换角度/降档 中文三值字面(#60禁翻译层); X2断言+报告轨迹段的证据链',
    ADD COLUMN source             VARCHAR(8)   NULL COMMENT
        '追问来源(C降级标注-审计面): kb=题池检索命中 / llm=AI现编; NULL=主题行。空手率日志=题池厚度免费仪表',
    ADD COLUMN kb_question_id     BIGINT       NULL COMMENT
        'source=kb 时的题池行 id(溯源); llm 行与主题行 NULL',
    ADD UNIQUE KEY uk_parent (parent_question_id) COMMENT
        '一父题至多一个追问孩子=B分叉幂等(09撞键第五次转世); 双击先查后生成, 撞键兜底';