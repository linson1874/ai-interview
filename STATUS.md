# STATUS.md — 项目实时状态账本

> 维护规则：**导师窗口**每日验收后更新「进度」；**技术支持窗口**解决问题后可在「问题日志」追加一行（日期 | 现象 | 根因 | 方案）。两窗口冲突时以本文件 + AGENTS.md 为准。

最后更新：2026-11-04 晚间（Day 2）

## 阶段
阶段一：环境与骨架

## 版本决策记录（重要，勿反复）
- 2026-11-04 查证：start.spring.io 当前默认 Boot 4.1.0；Boot 4.1.2 在 Central 存在；
  Spring AI 2.0.1 需 Boot>=4.0；**但 MyBatis-Plus 官方最新仍只有 spring-boot3-starter(3.5.7)，无 Boot4 starter**
- 裁决：**Boot 3.5.3 + MP 3.5.7**。Boot 4 留待秋招后作为"升级实战"亮点再上。

## 已完成 ✅
- Docker：`ai-mysql`(3306, Up) + `ai-redis`(6379, Up)；本机 MySQL 服务已停用
- 数据库 `ai_interview` 4 张表已建（含 uk_username 唯一索引）
- Maven 已配阿里云镜像（~/.m2/settings.xml）
- **backend 骨架由导师窗口生成**（Boot 3.5.3, 包 com.itlzy.aiinterview）：
  pom.xml / application.yml+application-dev.yml / 启动类 / HelloController /
  User实体 / UserMapper / MyMetaObjectHandler / UserMapperTest
- 根资产：sql/01_schema.sql(幂等化) / AGENTS.md / README.md / .gitignore

## 待验证 🔄（导师正在跑）
- [ ] mvn test 插入测试通过
- [ ] 服务启动 + GET /hello 返回 200
- [ ] git init + 首次提交

## 学员下一步（读码+业务起步，不再搭架子）
- [ ] IDEA 打开 backend，通读 9 个生成文件，能说出每个注解的作用（验收会提问）
- [ ] Day3 任务：手敲注册接口（DTO + BCrypt + DuplicateKeyException 处理）

## 欠账清单
- [ ] docker-compose 小作业三问（旧教程项目：起了几个服务 / rustfs 存什么 / 为何还留 MySQL）

## 待导师确认（技术支持窗口可写入建议，不许自行执行）
（空）

## 问题日志
- 2026-11-04 | 3306 bind 失败 | 本机 MySQL 服务占用 | 停服务后用 Docker 独占 3306
- 2026-11-04 | `^[[200~` 粘贴乱码 | Git Bash 不识别 bracketed paste | 复制命令/关 bracketed paste
- 2026-11-04 | `MYSQL_ DATABASE` 空格 | invalid reference format | 多行命令用复制，读报错定位镜像名异常
- 2026-11-04 | 向导生成 Boot 4.1.2 与 MP 生态不匹配 | 版本选型未看兼容矩阵 | 回落基线 3.5.3；教训已记入版本决策记录
