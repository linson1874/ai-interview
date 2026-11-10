-- ai-interview 增量脚本 03：t_interview 补出题数量列（合同06 count 窟窿修复，导师裁决方案 A：落表不落消息体）
-- 背景：异步路径把 count 弄丢了——DTO 校验 1~10（H6）→ 落库无列 → 消息体只有 interviewId（H1）→
--       消费者无从知道该出几题。count 是场次的固有属性（retry 重投天然沿用原题数），归表不归流。
-- 命名：question_count——count 是 SQL 保留字（COUNT(*) 那位），用它当列名从此条条 SQL 都要反引号伺候，避让。
-- 类型：TINYINT——合法域 1~10，表内先例 difficulty 同为 TINYINT；INT 是浪费四个字节，VARCHAR 是放弃比较能力。
-- 缺省语义：业务缺省的主源在 Service（dto.count==null → 5）；列的 DEFAULT 5 是防"绕过 Service 直插"
--       的保险丝，两处数字联动修改时记得它们是连体（/ai/questions 的 DEFAULT_COUNT=5 同族）。
-- 执行顺序：所有环境必须按 01 → 02 → 03 重放。

ALTER TABLE t_interview
    ADD COLUMN question_count TINYINT NOT NULL DEFAULT 5
    COMMENT '题目数量1-10(合同06 H6)；场次属性，缺省主源在Service，DEFAULT为保险丝' AFTER status;
