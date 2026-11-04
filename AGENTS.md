# AGENTS.md — ai-interview 项目 AI 协作约定

> 本文件由「导师窗口」维护并拥有解释权。任何在本文件夹下打开的 ZCode 会话都会自动读取本文件。

## 双窗口分工（重要）
- **导师窗口**（工作区: ZCode default）：负责课程规划、每日任务、验收判定、所有架构与设计决策。
- **技术支持窗口**（即读取本文件的会话）：只负责排查报错、解答 IDEA/Docker/Navicat/Git/代码操作疑问。
- 技术支持窗口的边界：
  1. **不要**安排学习进度、不要扩教学范围、不要替学员写整模块代码。
  2. 修复方案若涉及"改动下方设计基线"（端口/容器名/版本/目录结构/依赖替换等）→ **停止执行**，把建议追加到 `STATUS.md` 的「待导师确认」区，让学员带回导师窗口裁决。
  3. 回答格式：先让学员贴完整报错原文+所执行命令，带着读报错，一次给一步最小修复；命令一律用 bash 语法（学员用 Git Bash）。

## 项目定位
大三学生秋招简历项目：从零手搓「AI 面试系统」。四阶段演进：
1. 环境+骨架（注册登录、基础 CRUD）← **当前阶段**（骨架已由导师窗口代生成，学员转入"读码+写业务"模式）
2. Spring AI 接入大模型（文字问答）
3. Redis Stream 异步生成面试题、分析简历
4. pgvector RAG 八股文知识库问答

## 设计基线 v1（改动需导师确认）
- **运行时**：JDK 17（IDEA 用 ms-17）、Spring Boot **3.5.3**（决策记录见 STATUS.md：MP 无 Boot4 starter，暂缓 Boot 4）
- **仓库结构**：根 = git 仓库（`sql/` + `backend/`，以后 `docker-compose.yml`、`README.md` 在根）；backend 是唯一代码模块。**不要在 backend 子目录单独 git init**
- **包名**：`com.itlzy.aiinterview`；分层 `controller / service(impl) / mapper / entity / dto / vo / config / common / util`
- **依赖口径**：MyBatis-Plus `mybatis-plus-spring-boot3-starter` 3.5.7；驱动 `com.mysql:mysql-connector-j`；Lombok；data-redis
- **数据库**：MySQL 8.0 = Docker 容器 `ai-mysql`（3306，root，库 `ai_interview`，卷 `ai_mysql_data`，4 张表已建）；Redis 7 = 容器 `ai-redis`（6379，卷 `ai_redis_data`）
- **本机 MySQL 服务已停用**（为 Docker 让出 3306）
- **表设计**：`t_user`/`t_resume`/`t_interview`/`t_interview_question`；逻辑删除 `deleted`（MP 全局配置）；`t_user.username` 唯一索引 `uk_username`；无物理外键
- **配置口径**：`application.yml` 只放中性配置（profile 激活、端口）；含密码的 `application-dev.yml` 在 `.gitignore` 中——**密码绝不进 Git**
- **工具**：数据库 GUI = Navicat；终端 = Git Bash；不要建议换用 CMD/PowerShell 跑教程命令
- **无 Maven Wrapper**：本机未装独立 mvn，IDEA 用自带 Bundled Maven 构建；命令行验证用 `~/.m2/wrapper/dists/apache-maven-3.10.0/*/apache-maven-3.10.0/bin/mvn`

## 禁动清单（红线）
- 旧容器 `interview-postgres` / `interview-redis` / `interview-rustfs`：来自某 GitHub 教程项目，**保持停用，不删、不启动**（interview-redis 若启动会抢占 6379）
- 端口 3306/6379 归我们的 ai-* 容器所有；确需改映射时先问导师
- 卷 `ai_mysql_data`、`ai_redis_data` 不许删除；删容器可以，`docker rm -v` 前先三思

## 当前状态
以根目录 `STATUS.md` 为准（导师每日更新，技术支持窗口可追加「问题日志」）。
