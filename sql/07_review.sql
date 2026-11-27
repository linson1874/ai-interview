-- ============================================================
-- 07_review.sql — 复习域三表：复习单元 / 判分事件 / 掌握度曲线
-- 引擎: MySQL（行级高频更新+事务一致+同库 JOIN，零向量检索需求）
-- Redis 到期队列是运行时投影不落 DDL，真相源=t_review_item.next_review_at，投影可全量重建。
-- 时钟纪律: next_review_at/last_review_at/update_time 一律 Java 显式写入，
--   本表任何 DATETIME 列禁止 ON UPDATE CURRENT_TIMESTAMP。
-- ============================================================

-- ① 复习单元: 身份=场次原题行, 题库映射尽力而为, 状态机支持提交 CAS
CREATE TABLE IF NOT EXISTS t_review_item (
    id                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id               BIGINT   NOT NULL COMMENT '归属用户(t_user.id)',
    interview_question_id BIGINT   NOT NULL COMMENT '身份锚: 场次原题行(t_interview_question.id), 只读引用',
    kb_question_id        BIGINT   NULL COMMENT '题库行 id(PG kb_question.id), 检索映射成功才记, 失败 NULL——仅降级材料, 不降级身份',
    box_no                TINYINT  NOT NULL DEFAULT 1 COMMENT '间隔重复盒号 1-5',
    status                ENUM('due','processing') NOT NULL DEFAULT 'due' COMMENT '提交CAS状态机; processing 带租约防死锁, 回写后归 due',
    next_review_at        DATETIME NOT NULL COMMENT '到期时刻(Java 显式写入), Redis 投影分值的真相源',
    last_review_at        DATETIME NULL COMMENT '上次成功回写时刻——快照比对键: 回写 UPDATE 带 WHERE last_review_at=捞取快照',
    last_score            TINYINT  NULL COMMENT '上次判分 0-5, 首入队 NULL',
    deleted               TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除(MP 全局配置)',
    create_time           DATETIME NOT NULL COMMENT 'MP fill INSERT',
    update_time           DATETIME NOT NULL COMMENT 'Java 显式 set(update(null,wrapper) 不触发 MP fill), 兼作租约判据',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question (user_id, interview_question_id) COMMENT '一场题一个复习单元; 入队钩子撞键=幂等跳过(08 uk_username 同款)',
    KEY idx_user_due (user_id, next_review_at) COMMENT '今日到期: WHERE user_id=? AND status=? AND next_review_at<=?'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习单元(账本-状态层, ZSET 真相源)';

-- ② 判分事件: 只插入永不改，算法换代时的重算数据源
CREATE TABLE IF NOT EXISTS t_review_log (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    review_item_id BIGINT   NOT NULL COMMENT '归属复习单元(t_review_item.id)',
    score          TINYINT  NOT NULL COMMENT '判分 0-5',
    result         ENUM('promote','demote','freeze') NOT NULL COMMENT '三档转移结果: 前进/退回/冻结——语义落库',
    reviewed_at    DATETIME NOT NULL COMMENT '判分完成时刻(Java 显式写入), 窗口排序键',
    create_time    DATETIME NOT NULL COMMENT 'MP fill INSERT; 事件表无 update/deleted——只写不改',
    PRIMARY KEY (id),
    KEY idx_item_time (review_item_id, reviewed_at) COMMENT '窗口取最近 N 次: ORDER BY reviewed_at DESC LIMIT window'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习判分事件(账本-事件层, 投影重算之源)';

-- ③ 掌握度曲线: 聚合投影, 两轴各算各的
CREATE TABLE IF NOT EXISTS t_skill_profile (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '归属用户',
    axis         ENUM('tag','category') NOT NULL COMMENT '轴判别: tag=方向轴, category=题型轴——两轴不混算',
    axis_key     VARCHAR(32)  NOT NULL COMMENT '轴内键值(如 Java/MySQL 或 八股/项目/场景/HR)，不可变快照——源数据变更不回刷历史曲线',
    mastery      DECIMAL(5,2) NOT NULL COMMENT '掌握度 0-100 = 最近 window 次判分均值×20(本地算术)',
    sample_count INT          NOT NULL DEFAULT 0 COMMENT '累计事件数',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(MP 全局配置)',
    create_time  DATETIME     NOT NULL COMMENT 'MP fill INSERT',
    update_time  DATETIME     NOT NULL COMMENT 'MP fill INSERT_UPDATE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_axis_key (user_id, axis, axis_key) COMMENT '一人一轴一键一行; 判分同事务 upsert'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '技能掌握度曲线(投影层, 可从 t_review_log 全量重建)';

-- 数据侧诚实红线: 无 tags 的题(项目题/映射失败题)不进方向轴——宁缺毋滥, 此为特性非缺陷。
