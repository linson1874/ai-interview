-- =============================================================
-- AI面试系统 初始化脚本 v1
-- 执行方式: Navicat 右键库 -> 运行 SQL 文件, 或
--   docker exec -i ai-mysql mysql -uroot -p<密码> < sql/01_schema.sql
-- 约定: 脚本幂等(IF NOT EXISTS), 可重复执行; 后续变更以 02_xxx.sql 增量维护
-- =============================================================

CREATE DATABASE IF NOT EXISTS ai_interview
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_interview;

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt密文,禁止明文)',
    nickname    VARCHAR(32)           DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS t_resume (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    real_name     VARCHAR(32)   DEFAULT NULL COMMENT '真实姓名',
    target_job    VARCHAR(64)   DEFAULT NULL COMMENT '目标岗位',
    education     VARCHAR(64)   DEFAULT NULL COMMENT '学历,如:本科/211/计算机',
    skills        VARCHAR(1024) DEFAULT NULL COMMENT '技能标签,JSON数组字符串',
    self_intro    TEXT          DEFAULT NULL COMMENT '项目经历/自我介绍原文',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '简历表';

CREATE TABLE IF NOT EXISTS t_interview (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '面试用户ID',
    resume_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '关联简历ID',
    status        TINYINT  NOT NULL DEFAULT 0 COMMENT '状态:0生成中 1进行中 2已完成 3失败',
    overall_score DECIMAL(5,2) DEFAULT NULL COMMENT '总分(0-100)',
    summary       VARCHAR(1024)  DEFAULT NULL COMMENT 'AI对整场面试的总评',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试场次表';

CREATE TABLE IF NOT EXISTS t_interview_question (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    interview_id     BIGINT UNSIGNED NOT NULL COMMENT '所属面试场次ID',
    category         VARCHAR(32) NOT NULL COMMENT '类别:八股/项目/场景/HR',
    difficulty       TINYINT     NOT NULL DEFAULT 1 COMMENT '难度:1简单 2中等 3困难',
    question_content TEXT        NOT NULL COMMENT '题目内容',
    ref_answer       TEXT        DEFAULT NULL COMMENT 'AI生成的参考答案',
    user_answer      TEXT        DEFAULT NULL COMMENT '用户作答',
    score            TINYINT     DEFAULT NULL COMMENT 'AI评分:0-5',
    order_no         INT         NOT NULL DEFAULT 0 COMMENT '题目顺序',
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_interview_id (interview_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试题目表';
