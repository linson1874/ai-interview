-- ai-interview 增量脚本 02：t_interview.status 补中间态 4（已认领）
-- 背景：抢单翻 0→4（技术态"已认领"），完成走 4→1（业务态"题目就绪"），
--       1 号只表达业务就绪。
-- 本脚本零数据迁移：TINYINT 物理上本可存 4，改的是注释=枚举的唯一定义处。
-- 执行顺序：所有环境必须按 01 → 02 重放。

ALTER TABLE t_interview
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
        COMMENT '状态：0-生成中(排队)，4-生成中(已认领)，1-进行中(题目就绪)，2-已完成，3-失败';