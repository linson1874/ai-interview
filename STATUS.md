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

## 已验证 ✅（2026-11-04 晚，导师本机 Maven 实测）
- [x] mvn test：插入 t_user 成功（ID=1），自动填充时间生效，查询自动带 `WHERE deleted=0`
- [x] 服务启动：Tomcat 8080，`GET /hello` 返回 200 + 文案
- [x] git init + 首次提交；`application-dev.yml`（密码）确认未进版本库
- 待验证：学员在 IDEA 里复现（Bundled Maven 3 + JDK ms-17）

## 学员下一步（Day3 已验收，进入 Day4）
- [ ] 提交重构：`git add backend && git commit -m "refactor: 错误码枚举化+注册异常驱动; feat: 400 handler(裁决#3)"`（400 handler 加上后一起交）
- [ ] Day4：登录接口（Session vs JWT 方案先想好倾向，课上裁决）
- [ ] （欠账）Day2 五道读码题 + docker-compose 三问
- [ ] 日常：开机后 `docker start ai-mysql ai-redis`

## 欠账清单
- [ ] docker-compose 小作业三问（旧教程项目：起了几个服务 / rustfs 存什么 / 为何还留 MySQL）

## 待导师确认（技术支持窗口可写入建议，不许自行执行）
| # | 议题 | 提出方 | 导师裁决 | 状态 |
|---|---|---|---|---|
| 1 | 全局 handler 的 `Exception.class` 兜底把 404(`NoResourceFoundException`) 吞成 500，排查被误导；修法要给错误码表加新含义，文档"禁止私加" | 技术支持窗口 | **通过**：① 新增 `@ExceptionHandler(NoResourceFoundException.class)`→`code=404`+HTTP404，靠 Spring "就近匹配"抢回 404；② 补录编码规则"业务码个位/框架码镜像HTTP(404·405·500)"（文档升 v0.2）；③ 兜底 handler 加 `log.error` 打原始异常；④ 属 owner 批准的"补录"，不违反禁止私加 | 已放行，学员照裁决加 handler |
| 2 | 错误码魔法数字散落 Result 各处；`register` 返回 boolean 导致业务文案（"用户名已存在"）写进 Controller，违反文档 2.2 分层约定。技术窗口建议"缓改、另约重构课" | 技术支持窗口 | **改判：立即修，不排期**。① 定性：不是优化是"契约未兑现"（文档2.2本就要求 BusinessException 异常驱动）；② 新增 `common/ResultCode` 枚举收口所有码值；③ `register` 改 void+异常驱动，文案回 Service；④ DuplicateKeyException 兜底从"选做"升为"必做"（本条一起收掉）；⑤ 重构前提已具备：A-E 回归用例网 | ✅ 学员完成，2026-11-05 导师实测 A-E 全绿（还自创 BusinessException 携带 ResultCode，超出裁决范围的好设计） |
| 3 | Day3 导师验收实测发现：curl 在 Git Bash 发中文触发 Jackson `Invalid UTF-8 middle byte`（GBK 字节），`HttpMessageNotReadableException` 被 Exception 兜底吞成 500"系统繁忙"——协议层错误混进系统错误桶，且真凶一度被误判为"MySQL挂了" | 导师实测 | **裁决**：按 v0.2 已确立的"框架/协议层镜像 HTTP 码"规则补录 `code=400`；新增 `@ExceptionHandler(HttpMessageNotReadableException.class)`→HTTP400+code400，msg 提示"请求体不是合法JSON"。文档升 v0.3 | 待学员执行（两行 handler + ResultCode 加一项） |

## 问题日志
- 2026-11-04 | 3306 bind 失败 | 本机 MySQL 服务占用 | 停服务后用 Docker 独占 3306
- 2026-11-04 | `^[[200~` 粘贴乱码 | Git Bash 不识别 bracketed paste | 复制命令/关 bracketed paste
- 2026-11-04 | `MYSQL_ DATABASE` 空格 | invalid reference format | 多行命令用复制，读报错定位镜像名异常
- 2026-11-04 | 向导生成 Boot 4.1.2 与 MP 生态不匹配 | 版本选型未看兼容矩阵 | 回落基线 3.5.3；教训已记入版本决策记录
- 2026-11-04 | 404 被 `Exception.class` 兜底吞成 500 | `NoResourceFoundException` 属 Exception 家族；缺更具体的 404 handler | 加专用 handler→code=404+HTTP404；补录"框架码镜像 HTTP"编码规则(文档v0.2)。流程示范：技术窗口上报→契约owner裁决，不私改
- 2026-11-05 | 验收A/B诡异: A报500但库有数据,B重复却返回0 | 三连击: ①电脑重启容器全停(Exited) ②Git Bash curl 发中文=GBK字节→Jackson解析炸→500 ③导师Read到的是编辑器未保存旧缓冲,磁盘才是真相 | docker start 恢复; 中文入UTF-8文件用--data-binary @file; HEX()验证库内数据实为正确"李四"。教训:证据链>猜测,报错先读堆栈找 Caused by
- 2026-11-05 | 协议层JSON解析错误混进500桶 | HttpMessageNotReadableException无专用handler | 裁决#3: code=400镜像规则(文档v0.3),学员补两行handler
