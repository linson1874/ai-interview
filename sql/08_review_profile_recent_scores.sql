-- ============================================================
-- 08_review_profile_recent_scores.sql —— 合同09 第五步增量（G3 全局连续编号）
-- 引擎：MySQL（t_skill_profile 自家表，07 脚本出生 24 小时内加列，无历史包袱）
-- 为什么加列而不是 JOIN 重算：方向轴 tag 在 PG，MySQL 侧无法按轴找回
--   "该 tag 的全部事件"（跨引擎无 JOIN，G2 幽灵键条款的直接后果）。
--   窗口存进投影自身 = denormalize 换自包含，投影层经典手法。
-- 时钟纪律不变：本列纯数据，无时间语义。
-- ============================================================
ALTER TABLE t_skill_profile
    ADD COLUMN recent_scores VARCHAR(64) NULL COMMENT
        '窗口内最近N次分数CSV，左旧右新（如 2,0,3,4,4）；mastery=本列均值×20，截断由 ProfileWindow 执法'
        AFTER mastery;