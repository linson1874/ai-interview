# STATUS.md — 项目实时状态账本

> 维护规则：**导师窗口**每日验收后更新「进度」；**技术支持窗口**解决问题后可在「问题日志」追加一行（日期 | 现象 | 根因 | 方案）。两窗口冲突时以本文件 + AGENTS.md 为准。

最后更新：2026-11-06（Day 8）

## 路线图 v2（差异化拍板后）
1 骨架与用户体系 ✅ → 2 Spring AI 问答（进行中） → 3 Stream 异步出题+评分+报告 → 4 pgvector RAG+离线题池 → **5 差异化：提分闭环(ZSET间隔重复) → 追问决策器(CAT-lite)** → 6 选做(PDF解析/语音)
设计快照：`docs/product/differentiation.md`（D1-D3 已裁决）、`ai-feature-map.md`

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

## 学员下一步（阶段三第1课：合同06 v1.0 已放行，开工令）
- [x] H1~H6 评审完毕：本项目最高水位答卷（H3/H4/H5/H6 多段原文入宪）；导师执法 R1中间态4(R3抢单撞状态机语义)/R2护栏按上沿5000(用学员自己的"碰平口径"原则)/R3 T6T8代填/R4 MAXLEN修剪补刀
- [ ] 按 §5 顺序实现：sql/02 增量脚本打头；T4 kill -9 演练必须留证据
- [ ] 中期结账：八题 + JwtUtilTest整改 + converter纯单测
- [ ] 日常：开机后 `docker start ai-mysql ai-redis`

- [x] 预习题（消费者宕机任务去哪）已答：PEL/XAUTOCLAIM/定时清扫机制全对，B+——批注两处盲区入合同06设计种子：①at-least-once→重复执行→幂等门(H3) ②delivery-count 死信需自建(H4)
- [ ] **合同06 H1~H6 拍板**（H3 幂等门是灵魂，必须写出 UPDATE 形状+抢输者ACK与否的理由）→ 评审 v1.0 → 实现
- [ ] 实现后必跑 T4 故障演练（kill -9 验证幂等）与 T5 毒消息演练——**本课的验收一半在"故意把系统弄坏"**
- [ ] 小尾巴（Day8遗留，随本课交）：JwtUtilTest 断言整改+负路径 / converter 纯单测 / 八题门票
- [ ] 日常：开机后 `docker start ai-mysql ai-redis`

## 欠账清单
- [ ] docker-compose 小作业三问（旧教程项目：起了几个服务 / rustfs 存什么 / 为何还留 MySQL）

## 待导师确认（技术支持窗口可写入建议，不许自行执行）
| # | 议题 | 提出方 | 导师裁决 | 状态 |
|---|---|---|---|---|
| 1 | 全局 handler 的 `Exception.class` 兜底把 404(`NoResourceFoundException`) 吞成 500，排查被误导；修法要给错误码表加新含义，文档"禁止私加" | 技术支持窗口 | **通过**：① 新增 `@ExceptionHandler(NoResourceFoundException.class)`→`code=404`+HTTP404，靠 Spring "就近匹配"抢回 404；② 补录编码规则"业务码个位/框架码镜像HTTP(404·405·500)"（文档升 v0.2）；③ 兜底 handler 加 `log.error` 打原始异常；④ 属 owner 批准的"补录"，不违反禁止私加 | 已放行，学员照裁决加 handler |
| 2 | 错误码魔法数字散落 Result 各处；`register` 返回 boolean 导致业务文案（"用户名已存在"）写进 Controller，违反文档 2.2 分层约定。技术窗口建议"缓改、另约重构课" | 技术支持窗口 | **改判：立即修，不排期**。① 定性：不是优化是"契约未兑现"（文档2.2本就要求 BusinessException 异常驱动）；② 新增 `common/ResultCode` 枚举收口所有码值；③ `register` 改 void+异常驱动，文案回 Service；④ DuplicateKeyException 兜底从"选做"升为"必做"（本条一起收掉）；⑤ 重构前提已具备：A-E 回归用例网 | ✅ 学员完成，2026-11-05 导师实测 A-E 全绿（还自创 BusinessException 携带 ResultCode，超出裁决范围的好设计） |
| 3 | Day3 导师验收实测发现：curl 在 Git Bash 发中文触发 Jackson `Invalid UTF-8 middle byte`（GBK 字节），`HttpMessageNotReadableException` 被 Exception 兜底吞成 500"系统繁忙"，真凶一度被误判为"MySQL挂了" | 导师实测 | **裁决**：补录 `code=400`（镜像规则）；新增 `HttpMessageNotReadableException` handler | ✅ 学员已完成(e0b4d23) |
| 4 | Knife4j 接口文档页要不要接（Day4 选做项，属依赖口径） | 学员/支持窗口 | **批准接入文档能力，但选型更换**：实测 springdoc-openapi 2.8.6 × Boot 3.5.3 全绿(启动0错、/v3/api-docs 200、swagger-ui 200)；knife4j 4.5.0 内封老 springdoc(Boot 3.0/3.1 时代)，硬配兼容雷区——先用官方 springdoc，knife4j 新版适配后再考虑换壳。拦截器只护 /user/**，文档路径无需放行 | ✅ 完成(b7c0f8b)：实测 /v3/api-docs 200、swagger-ui 200 |
| 5 | 单端互踢做不做（同账号新登录使旧 token 失效） | 学员/支持窗口 | **做**。设计：Redis 单键 `login:user:{userId}`=当前token，登录覆盖写=天然踢旧，校验"存根等于本token"，logout 按属性 userId 删键；顺带修复审查瑕疵A(Redis TTL 硬编码 Duration.ofHours(2) 与 yml jwt.expire-minutes 双本账→同源注入)。合同02已升 **v0.2**(用例L生效)。多端在线(SET方案)作为面试谈资留档不做 | ✅ 完成(b7c0f8b)：实测用例L(旧票401/新票200)、TTL=7199≈同源120min、注销K通过。遗留观察：旧格式键 login:token:* 靠TTL自然死亡，生产级键迁移(双读/灰度)记为面试谈资 |
| 6 | 简历模块合同：学员首次起草(D1-D5) | 学员 | **评审放行 v1.0(e290f44→本版)**。采纳:D1a/D2b/D5防枚举思路;修正4处:①D1b误答401(与自身D5矛盾,改统一404) ②"MP默认上限500"为讹传(实为无默认,合同钉50+maxLimit显式) ③D4虚构字段status(t_resume无此列,删) ④姓名正则过严+selfIntro选填null语义封死 | 合同生效,学员进入实现 |
| 7 | 学员实现前**复审合同**发现：§2提示语"见4"与§4"见2"循环引用，文案实际悬空；并提议 @Size 可重复注解拆上下限各配文案 | 学员 | **批准补录**：新增 §2.1 提示语定稿表(11条)；采纳可重复@Size(Bean Validation 2.0)，附注"同字段多违规取第一条FieldError、顺序不承诺"与"skills先trim后判空"两条实现规则。合同升 **v1.1** | ✅ 生效，实现以v1.1为准 |
| 8 | Day6 实现评审：①PUT null清空语义留白 ②分页文案暂拟 ③排序未明写（学员提报）+ 实测新发现④删他人活跃简历→404 构成枚举侧信道（与学员自身D5矛盾）| 学员/导师实测 | **①全量替换**(D6: 表单提交场景"删空再存"是真需求; MP updateById 跳null实测复现, 落地开关=FieldStrategy.IGNORED) **②文案定稿入§2.1** **③批准createTime desc+id desc**(次键防翻页漂移) **④D7: 条件删除一条SQL,一律code0**(双查竞态+侧信道一并消除)；另批注: 学员handler"码值≥400同步HTTP状态"写法予以认可；学员改§2.1 min文案获追认；成功msg统一"ok"新约定 | 合同v1.2生效，Day6b修复清单见§6.5 |
| 9 | 阶段二供应商选型：学员提议百炼为主、后续加 DeepSeek/GLM 选项 | 学员 | **批准**。架构原则定档：**只依赖 OpenAI 兼容协议，不绑定厂商**（切供应商=改配置）。百炼 compatible-mode + qwen-plus 起步；百炼模型广场自带 deepseek/glm 等第三方模型，可能一个 key 通吃——三个模型 ID 待学员控制台实测回填。key 走环境变量 AI_API_KEY（红线延续）；成本护栏第一档=登录保护+max-tokens512，限流留阶段三前裁决 | 合同04 v0.1生效，Day7实现 |
| 10 | Day7 审查补充裁决：学员对"中立单key命名"提出反驳（目标=多厂商key并存+模型可选） | 学员 | **修正#9的环境变量规则**：厂商前缀命名合法（多key并存场景），老红线不变；补分层注解：部署级切换（现在）vs 运行时多模型路由（Day8 与简历出题一起设计，合同04升v0.2） | ✅ 生效；Day7缺口清单 A四件套/B拦截/C上游收口/E付费测试@Disabled 仍待学员交付 |

## 问题日志
- 2026-11-04 | 3306 bind 失败 | 本机 MySQL 服务占用 | 停服务后用 Docker 独占 3306
- 2026-11-04 | `^[[200~` 粘贴乱码 | Git Bash 不识别 bracketed paste | 复制命令/关 bracketed paste
- 2026-11-04 | `MYSQL_ DATABASE` 空格 | invalid reference format | 多行命令用复制，读报错定位镜像名异常
- 2026-11-04 | 向导生成 Boot 4.1.2 与 MP 生态不匹配 | 版本选型未看兼容矩阵 | 回落基线 3.5.3；教训已记入版本决策记录
- 2026-11-04 | 404 被 `Exception.class` 兜底吞成 500 | `NoResourceFoundException` 属 Exception 家族；缺更具体的 404 handler | 加专用 handler→code=404+HTTP404；补录"框架码镜像 HTTP"编码规则(文档v0.2)。流程示范：技术窗口上报→契约owner裁决，不私改
- 2026-11-05 | 验收A/B诡异: A报500但库有数据,B重复却返回0 | 三连击: ①电脑重启容器全停(Exited) ②Git Bash curl 发中文=GBK字节→Jackson解析炸→500 ③导师Read到的是编辑器未保存旧缓冲,磁盘才是真相 | docker start 恢复; 中文入UTF-8文件用--data-binary @file; HEX()验证库内数据实为正确"李四"。教训:证据链>猜测,报错先读堆栈找 Caused by
- 2026-11-05 | 协议层JSON解析错误混进500桶 | HttpMessageNotReadableException无专用handler | 裁决#3: code=400镜像规则(文档v0.3),学员补两行handler
