-- ai-interview 增量脚本 03：t_interview 补出题数量列
-- 背景：异步路径把 count 弄丢了——DTO 校验 1~10 → 落库无列 → 消息体只有 interviewId，
--       消费者无从知道该出几题。count 是场次的固有属性，归表不归消息。
-- 命名：question_count——count 是 SQL 保留字，避让。
-- 类型：TINYINT——合法域 1~10；缺省主源在 Service（未传 → 5），列的 DEFAULT 5 是防绕过直插的保险丝。
-- 执行顺序：所有环境必须按 01 → 02 → 03 重放。

ALTER TABLE t_interview
    ADD COLUMN question_count TINYINT NOT NULL DEFAULT 5
    COMMENT '题目数量1-10(合同06 H6)；场次属性，缺省主源在Service，DEFAULT为保险丝' AFTER status;
