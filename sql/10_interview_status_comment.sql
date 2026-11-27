-- ============================================================
-- 10_interview_status_comment.sql —— t_interview.status 值域 COMMENT 对齐
-- 背景：01_schema.sql 只写 0/1/2/3，缺 4（消费者认领态）与 5（已取消，合法入边{0,4}->5）。
-- 零新列：取消时刻=update_time（DB 钟自维护），仅修 COMMENT，一行数据不动。
-- 两代制自宣：t_interview 属存量代——update_time 由 ON UPDATE CURRENT_TIMESTAMP 自维护。
-- 执行顺序：所有环境必须按 01 → 02 → 03 → 04 → 07 → 08 → 09 → 10 重放。
-- ============================================================
ALTER TABLE t_interview
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
    COMMENT '状态:0排队待领取 4生成中(已认领) 1进行中 2已交卷 3失败 5已取消(合法入边{0,4}->5)';
