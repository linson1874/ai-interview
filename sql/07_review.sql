-- ============================================================
-- 07_review.sql — 合同09《提分闭环》复习域（决策点 D 定稿 + F2 执法件）
-- 编号口径: G3 法定"全局连续、目录分引擎"——本份为 MySQL 目录第 5 份、全局第 7 份。
-- 引擎判定: MySQL。复习域=行级高频更新+事务一致+同库 JOIN，零向量检索需求——
--   双引擎分工旧判据(K2): 业务与交易归 MySQL, 语义与向量归 PG。
-- PG 判断: 本合同不新建 PG 脚本。复习链对 PG 的唯一依赖=只读 kb_question.answer_text
--   拼材料(05/06 现成资产), 不新增表。
-- Redis 判断: 到期队列 ZSET review:queue 是运行时投影, 不落 DDL;
--   真相源=t_review_item.next_review_at, 投影可全量重建(06 家法: 队列可死, 账本不死)。
-- F1 执法: v1 复习判分只写 t_review_log, 一行不碰 t_interview_question——
--   本脚本不给 07 域的任何表加列(question_type 列归 v2, 与 07 升版同批)。
-- 时钟纪律(#15 移植): next_review_at/last_review_at/update_time 一律 Java 单时钟写入,
--   本表任何 DATETIME 列禁止 ON UPDATE CURRENT_TIMESTAMP; stale 租约比较由 Java 算好传参。
-- ============================================================

-- ① 复习单元: 身份=场次原题行(A 裁决), 聚合键 kb_question_id 尽力而为, 状态机=F2①
CREATE TABLE IF NOT EXISTS t_review_item (
    id                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id               BIGINT   NOT NULL COMMENT '归属用户(t_user.id)',
    interview_question_id BIGINT   NOT NULL COMMENT '身份锚: 场次原题行(t_interview_question.id), 只读引用不加列(F1)',
    kb_question_id        BIGINT   NULL COMMENT '聚合键: 题池行(PG kb_question.id), 检索映射 top1 且 d<=0.3 才记, 失败 NULL——仅降级材料, 不降级身份',
    box_no                TINYINT  NOT NULL DEFAULT 1 COMMENT 'Leitner 盒号 1-5(B 裁决: >=4前进/<=2回盒一/=3冻结)',
    status                ENUM('due','processing') NOT NULL DEFAULT 'due' COMMENT 'F2① 提交CAS状态机; processing 带租约(死锁防护, 待导师追认), 判分回写后归 due',
    next_review_at        DATETIME NOT NULL COMMENT '到期时刻(Java 单时钟), ZSET score 的真相源',
    last_review_at        DATETIME NULL COMMENT '上次成功回写时刻——F2② 快照比对键: 回写 UPDATE 带 WHERE last_review_at=捞取快照',
    last_score            TINYINT  NULL COMMENT '上次判分 0-5(07 评分口径), 首入队 NULL',
    deleted               TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除(MP 全局配置)',
    create_time           DATETIME NOT NULL COMMENT 'MP fill INSERT',
    update_time           DATETIME NOT NULL COMMENT 'Java 单时钟显式 set(update(null,wrapper) 不触发 MP fill——09 双击演练实锤修复), claim 时刻的载体=租约判据',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question (user_id, interview_question_id) COMMENT '一场题一个复习单元; 入队钩子撞键=幂等跳过(08 uk_username 同款)',
    KEY idx_user_due (user_id, next_review_at) COMMENT '今日到期: WHERE user_id=? AND status=? AND next_review_at<=?'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习单元(账本-状态层, ZSET 真相源)';

-- ② 判分事件: 只插入永不改——G1 三表制的存在理由, v2 算法换代的口粮
CREATE TABLE IF NOT EXISTS t_review_log (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    review_item_id BIGINT   NOT NULL COMMENT '归属复习单元(t_review_item.id)',
    score          TINYINT  NOT NULL COMMENT '判分 0-5(07 评分链原样产出)',
    result         ENUM('promote','demote','freeze') NOT NULL COMMENT 'B 三档转移结果: >=4前进/<=2回盒一/=3冻结——语义落库, 曲线不靠猜',
    reviewed_at    DATETIME NOT NULL COMMENT '判分完成时刻(Java 单时钟), 窗口排序键',
    create_time    DATETIME NOT NULL COMMENT 'MP fill INSERT; 事件表无 update/deleted——只写不改的家法用表结构封死',
    PRIMARY KEY (id),
    KEY idx_item_time (review_item_id, reviewed_at) COMMENT '窗口取最近 N 次: ORDER BY reviewed_at DESC LIMIT window'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习判分事件(账本-事件层, 投影重算之源)';

-- ③ 掌握度曲线: 聚合投影, 两轴各算各的(E1 铁律表结构形态)
CREATE TABLE IF NOT EXISTS t_skill_profile (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '归属用户',
    axis         ENUM('tag','category') NOT NULL COMMENT '轴判别: tag=方向轴(kb_question.tags 快照), category=题型轴(07 四值)——两轴不混算, E1 家法',
    axis_key     VARCHAR(32)  NOT NULL COMMENT '轴内键值(MySQL/Redis... 或 八股/项目/场景/HR)。G2: 不可变快照——PG 改 tag 不回刷历史曲线, 跨引擎幽灵键条款',
    mastery      DECIMAL(5,2) NOT NULL COMMENT '掌握度 0-100 = 最近 window 次判分均值×20(W8 golden rule: 本地算术, 模型不算数)',
    sample_count INT          NOT NULL DEFAULT 0 COMMENT '累计事件数, 曲线置信度注脚(样本稀的 62% 和样本厚的 62% 不是一回事)',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(MP 全局配置)',
    create_time  DATETIME     NOT NULL COMMENT 'MP fill INSERT',
    update_time  DATETIME     NOT NULL COMMENT 'MP fill INSERT_UPDATE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_axis_key (user_id, axis, axis_key) COMMENT '一人一轴一键一行; G4 判分同事务 upsert'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '技能掌握度曲线(投影层, 可从 t_review_log 全量重建)';

-- 数据侧诚实红线(注释即合同): 无 tags 的题(项目题/映射失败题)不进方向轴——宁缺毋滥, 此为特性非缺陷。
