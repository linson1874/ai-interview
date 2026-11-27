-- 04_add_report.sql —— 报告状态机 + 结构化报告位 + 评分简评列
-- ① t_interview：报告小状态机（主状态 0-4 语义零改动）
ALTER TABLE t_interview
    ADD COLUMN report_status TINYINT     NOT NULL DEFAULT 0 COMMENT '报告状态:0待报 1生成中 2就绪 3失败',
    ADD COLUMN report_json   TEXT        DEFAULT NULL       COMMENT '结构化报告JSON(评分输出序列化落库)';

-- ② t_interview_question：评分简评列（列名避开 MySQL 关键字 comment）
ALTER TABLE t_interview_question
    ADD COLUMN score_comment VARCHAR(512) DEFAULT NULL COMMENT 'AI评分简评(与score/ref_answer同批回写)' AFTER score;