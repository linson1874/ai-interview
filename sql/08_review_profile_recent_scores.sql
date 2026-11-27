-- ============================================================
-- 08_review_profile_recent_scores.sql —— t_skill_profile 加最近判分窗口列
-- 引擎：MySQL
-- 为什么加列而不是 JOIN 重算：方向轴 tag 在 PG，跨引擎无 JOIN 无法按轴找回全部事件，
--   窗口存进投影自身（冗余换自包含）。
-- 时钟纪律不变：本列纯数据，无时间语义。
-- ============================================================
ALTER TABLE t_skill_profile
    ADD COLUMN recent_scores VARCHAR(64) NULL COMMENT
        '窗口内最近N次分数CSV，左旧右新（如 2,0,3,4,4）；mastery=本列均值×20，截断由 ProfileWindow 执法'
        AFTER mastery;