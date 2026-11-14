# STATUS.md — 项目实时状态账本

> 维护规则：**导师窗口**每日验收后更新「进度」；**技术支持窗口**解决问题后可在「问题日志」追加一行（日期 | 现象 | 根因 | 方案）。两窗口冲突时以本文件 + AGENTS.md 为准。

最后更新：2026-11-06（Day 8）

## 路线图 v2（差异化拍板后）
1 骨架与用户体系 ✅ → 2 Spring AI 问答（进行中） → 3 Stream 异步出题+评分+报告 → 4 pgvector RAG+离线题池 → **5 差异化：提分闭环(ZSET间隔重复) → 追问决策器(CAT-lite)** → 6 选做(PDF解析/语音)
设计快照：`docs/product/differentiation.md`（D1-D3 已裁决）、`ai-feature-map.md`

**前端定档（2026-11-09 补，回应学员提问）**：不随功能同步做；**阶段三收尾后插一周"前端冲刺周"**——届时核心闭环（注册登录→简历→异步面试→作答→评分报告）完整，页面才有演示价值；阶段四/五功能逐个回填页面。技术栈 Vue3+ElementPlus，页面骨架 AI 辅助生成，**接口对接与状态逻辑必须学员亲手写**。冲刺周前置资产=工单⑥ `scripts/e2e.sh`（回归脚本入仓，在此之前它就是"验收面"）。

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

## 学员下一步（阶段三收官：四洞修复单 → 门票 → 阶段四开机）
- [ ] 四洞修复单（半天）：#29 HttpRequestMethodNotSupportedException→405+code405 handler；#30 降级返回 code=1（S13）；#31 SCORING_MAX_TOKENS 1200+prompt 加 ref≤200 句（S14）；#32 报告材料 150 字截断落地
- [ ] 每洞配免费单测（MockMvc 打错方法测 405；S13/S14 断言；截断纯函数）——converter 式测试债顺势全还
- [ ] 工单⑥ scripts/e2e.sh 入仓（收编 A~R/P/INJ 剧本）
- [ ] **阶段四门票**：Day2 五读码题 + compose 三问 + JwtUtilTest 裸 assert（第六次点名，surefire 带 -ea 会掩盖它在 IDE 里的假绿）
- [ ] 门票+四洞齐 → 合同 08 发布：pgvector 题池+检索+embedding 选型（J4 的 CAT 校准话从这开始兑现）
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
| 11 | 抢单边 IN(0,4) 补丁（原 0 单边在崩溃恢复死锁，D1 演练实锤永卡4） | 学员（带证据） | R6 批准——单线程串行+min-idle裕量+先删后插三柱撑腰；扩容前必须换 update_time 租约CAS（合同06 v1.2 §1 写死）；连带 R8 min-idle 300→600s | ✅ 裁决入合同06 v1.2；学员改参数 |
| 12 | poll NOGROUP 自愈 vs 显式 MKSTREAM | 学员 | 批准为架构行为（双层防御：启动 ensureGroup + 运行期自愈；不赌未验证字节码）；微瑕两处批注（catch 归因日志/死信边虚报）随工单修 | ✅ 裁决入合同06 v1.2 |
| 13 | status=0 孤儿行（DEL 火化消息，活标本 id=1、2） | 学员 | 三选项全否，裁 R7 **事务性发件箱补投**（@Scheduled 60s 扫 0+stale → enqueueWithCompensation）；retry 保持用户语义；id=1、2 自愈即验收 | ✅ 已实现，导师实测 id=1、2 自愈通过(2865677 验收) |
| 14 | 合同07 J1~J5 学员拍板（附 6 项请示） | 学员 | **评审放行 v1.0**：J1 混合形态/J2-a 同批评分"同头口径"王牌/J4 三条款+演示口径表/J5 本体论序言+三不+五层——全文入宪；执法：S1 三字段纠"4字段"笔误、S2 连点窗口裁**答案一致性闸**（否 version 列、否裸 LWW）、S3 evidence 带、S4 手动重生、S5 权重 1:2:3、S6 XSS 移交前端合同 | ✅ v1.0 生效，开工顺序见合同 §6 |
| 15 | **时区暗雷（头号案，④-3 开工前置）**：MySQL 容器时区=UTC，update_time 由表上 `ON UPDATE CURRENT_TIMESTAMP`（服务端 UTC）写，create_time 由 MP fill（Java 本地）写——同列两时钟差 8h（实锤：qid61 create=20:00:53 / update=12:19:37）。波及：①消费者 sweep min-idle 600s **形同虚设**（update_time 永远"老8小时"，入 PEL 即可被抢——D2 演练 40s 抢单真因，见 #20 翻案）②R7 发件箱 `lt(now-60s)` 恒真（靠 status=0 条件才没误伤）③展示时间失真。选项：α 容器加 TZ=Asia/Shanghai（一处根治，动 ai-mysql=设计基线）/ β 所有 wrapper UPDATE 手动 set updateTime / γ 接受 UTC 全链换算（最绕）。技术窗口推荐 α | 技术支持窗口（带验尸证据） | ⏸ 待裁；④-3 消费者 sweep 写法取决于本条 |
| 16 | **报告 prompt 初稿评审**：合同07 §4 只定稿了评分 prompt，报告 prompt 属合同空白。技术窗口按 J3 骨架代拟6条初稿（含：不做算术声明/四值枚举/`<attempted_answers>` 定界=J5①②平移到报告链二次注入面/材料模板不含 refAnswer）+weakPoints 复数拼写统一（schema/prompt/JSON 三方一致）。冒烟实测首轮即过、质量佳 | 技术支持窗口 | ⏸ 请导师逐字评审，通过后入合同成逐字件（同 §4 纪律） |
| 17 | **评分 temperature=0.2**：P1 同答案重评 4→5 分漂移（采样模型非函数，4/5 边界本就模糊）。落地：评分调用锁 0.2（可复现性优先），出题保持默认（要多样性）——callUpstream 第四参。合同未规定 temperature 口径 | 技术支持窗口（学员拍板选 a） | ⏸ 知情追认；另建议入 J5 诚实边界注记（LLM-as-judge 公认局限，简历加分点） |
| 18 | **06 §2 questions[] 形状勘误**：轮询面题目对象缺 `id` 字段——07 作答端点 `/answer/{qid}` 的隐含推论（列表要可引用必带身份证），代码已落地（工单③ QuestionItemVO+装配补 setId），文档未改 | 技术支持窗口 | ⏸ 请导师补录 06 §2 形状说明（非新决策，是漏项） |
| 19 | **Day7 提交信息"context-path 改 /api"与现状不符**：两份 yml 均无 context-path，/api 前缀不存在；本窗口据此误导验收 URL 打空 404"接口不存在"。请导师核查历史（是否后来撤了/提交信息预支未实施计划），并统一口径：本项目无路径前缀 | 技术支持窗口 | ⏸ 待核 |
| 20 | **T5 演练方式注记 + D2 翻案**：①prompt 投毒≠毒消息（删 §2 第6条后 JSON 仍有四层保险：prompt指令/getFormat/JSON mode/Lenient解析，正品埋法=代码入口 throw）；②D2 三集里 40s 即发生抢单，当时归因"清扫周期到"，实为 #15 时区差使 min-idle 恒满足——演练结论有效但机理需注记 | 技术支持窗口 | ⏸ 请导师在 06 D2 条款补注（演练资产保留，机理改写） |
| 21 | **causeContains 异常链修复 + NOGROUP 自愈翻案**：v1.2 微瑕①原方案 `e.getMessage().contains("BUSYGROUP")` 实测无效——Lettuce 错误被 Spring Data 包成 `RedisSystemException("Error in execution")`，字样在 cause 根因；已改沿 cause 链遍历。连带翻案：poll 的 NOGROUP 自愈同款判断从未命中过（昨日刷屏案实靠手动 XGROUP CREATE 救场） | 技术支持窗口（已落地编译绿） | ⏸ 请导师更新 06 微瑕批注；纪律入库：判断异常看 Caused by 身份证 |
| 22 | **S7 score_comment 列**：合同 P1"三值同列"但题目表无 comment 容身列（R5 同款案件开工日发现），sql/04 已连带补 `t_interview_question.score_comment VARCHAR(512)`（列名避开 MySQL 关键字） | 技术支持窗口 | ⏸ 请导师追认进 07 修订表（开工日发现 vs 合同未点破） |
| 23 | **交接纪律新增：敲完先 Ctrl+S Save All**：11-12 ④-2 三连编译错（接口找不到声明/converter 空壳），实为 IDEA buffer（新）与磁盘（旧）分叉——Read 工具见 buffer、mvn 见磁盘，SearchReplace 三连匹配失败同根。与 11-05"导师 Read 到未保存旧缓冲"案互为镜像 | 技术支持窗口 | ⏸ 请导师追加进 AGENTS.md："你敲完→我编译/验收"交接点必须先保存 |
| 24 | **CommandLine JAVA_HOME 口径重申**（旧提未落）：命令行 mvn 必须前缀 `$env:JAVA_HOME="C:\Users\Lying\.jdks\ms-17.0.19"`（系统默认 JDK25 毒 Lombok），且 mvn 真值路径含 64 位 hash 永不手敲（省略号占位符被拄进命令实案×2）；建议 AGENTS.md 工具栏补这两条 bash/powershell 口径 | 技术支持窗口 | ✅ 已入 AGENTS（2026-11-13 五条口径：时钟单源/无前缀/SaveAll/mvn路径/异常身份证） |
| 15-24 | **批裁（导师，2026-11-13；证据实查：容器 date=UTC05:45 vs Java13:45 / qid61 两时钟 / git 全史无 /api / score_comment 有 sql/04 可溯）** | 导师 | **#15=α** 容器带 TZ=Asia/Shanghai 重建（β 否：人肉纪律不如机制根治；γ 记国际演进）。**机制勘误：①sweep min-idle 用 Redis PEL 计时与 DB 时区无关——此罪不成立；②outbox 实为 Java 写钟行恒假(救不到新孤魂)/DB 写钟行正常——D2 翻案不背书，T12 复测再定**。**#16 批准**：报告 prompt 定稿入 07 §4.1（五要点+三注：摘录150截断/空数组语义/护栏句），逐字稿有出入持原文碰平。**#17 追认**+J5 边界⑤。**#18 补录** 06 §2。**#19 定稿**无路径前缀，举证按预支事实立案。**#20 采纳** T5 勘误入 06。**#21 追认** causeContains；自愈从未命中→T13 DEL 实弹。**#22 追认** S7 入 07+comment≤50字附注。**#23/24 入 AGENTS** | ✅ 全清；④-3 解锁（前置=α+T12/T13） |
| 25-32 | **批裁（导师，2026-11-14，iid=23 实弹全链复验）** | 导师/实弹 | **#25 追认**：@Size(1000) 已在码，三层防御成形，J5③注记更新。**#26 追认**（S11 rebuild 0边合法：report 链唯一救路+抢单防双钱）。**#27 结案背书**：D2=人肉重启链路耗时，sweep/时区两被告均无罪。**#28 盖章**：§4.1 换实弹文本为逐字件（三处差异勘平，S12）。**#29 新立**：方法不匹配 POST 撞 GET /{id} 被兜底吞 500——违"框架错镜像 HTTP"家法，裁 405 handler，01 升 v0.4。**#30 新立**：q96 降级返回 code=0 三null=静默欺骗，裁 S13（code=1+可重评语）。**#31 新立**：q96 病根=SCORING_MAX_TOKENS 800 腰斩 refAnswer（JsonEOF 铁证；05 复测注预言三期重演），裁 S14（1200+ref≤200字句）。**#32 新立**：§4.1 注①材料150字截断代码未兑现——合同撒谎，落地或当日改注 | ✅ 四洞入修复单 |
| 25 | **J5③"长度护栏"合同文字与实现的 gap**：INJ-4 实弹（三百余字复读）畅通直达模型（code=0 非校验拒，2.65s 被消化为"弃答"评 0 分）——AnswerDTO 现仅 @NotBlank，参数层无 @Size 上限；prompt 层护栏实测够用 | 技术支持窗口（带 INJ-4 截图） | ⏸ 是否补 @Size(max=?) 属设计基线，请导师裁：补参数护栏 / 维持现状并在 07 J5③ 注记"护栏在 prompt 层" |
| 26 | **rebuild CAS `IN(0,2,3)→0` 扩展申报**：S4 原文只列 2/3 两入口，实现多了 0——救"CAS 后 XADD 前崩溃"的孤魂（report 链无发件箱，rebuild 是唯一救路；消费者抢单 CAS IN(0,1) 保证不烧双份钱）。代码注释已写明理由 | 技术支持窗口 | ⏸ 请导师过目追认（07 S4 修订表补 0 边，或裁掉 0 改配 outbox） |
| 27 | **D2 定论提交（#20 留置半句的结案证据）**：α 重建后 T12/T13/D2 三演练全过——新场次两时钟同钟(13:57:30/35)、outbox 60s 施救祭品19、对 status=4 免疫(布景130s 沉默=正证)、NOGROUP 自愈首战实弹(144ms 会师)、恢复模式 3s 接管(死亡14:21:25→接管14:22:22)。旧"40s 抢单"案结案=当年"杀进程→IDEA 重启→恢复模式接管"人肉链路耗时，与 sweep 周期和时区均无关 | 技术支持窗口（带日志时间戳） | ⏸ 请导师在 #20 项下结案背书（当时原话"翻案不背书，T12 复测再定"——复测已交卷） |
| 28 | **§4.1 报告 prompt 实战验证反馈**：定稿后首战三场（18/22/20）全部首轮过闸零 L3 重试；INJ 五发三不全过（伪造定界符被当数据、指令零执行、报告链把注入写进薄弱点评估"规范意识存在问题"）；golden rule 反证双场吻合（22场 53.33、20场 7.50 手工复算分毫不差）。建议 §4.1 加注"经实战验证(2026-11-13)" | 技术支持窗口（带六张截图档） | ⏸ 请导师盖章追认 |

## 问题日志
- 2026-11-14 | q96 评分降级根因：JsonEOF expecting closing quote | SCORING 800 护栏腰斩长 refAnswer——护栏即故障源（05 复测注预言的重演） | S14: 1200+简洁条款+截断落地
- 2026-11-14 | POST /interview/finish 路径歧义撞 GET /{id} 被吞 500 | HttpRequestMethodNotSupported 无专属 handler，违框架镜像家法 | #29→01 v0.4 405 行+handler，配 MockMvc 测试
- 2026-11-14 | 实弹正面案 iid=23：2 评 1 降级→overall=NULL 走样本闸、报告只定性、renormalize 生效 | J4 三条款实战首验通过 | —（条款活着）
- 2026-11-13 | 工单④收官日三案：①α重建后捞 dev.yml 密码正则二次取用 1045（卷内原密码与 yml 新密码分裂，全新初始化卷时会引爆）②ReportDetailVO 改型时手术刀滑到隔壁 ReportVO（overallScore 误杀成 Integer score），编译器当场逮住③第二刀 buffer/磁盘分叉二进宫（敲完未再存） | ①已存在卷不认 MYSQL_ROOT_PASSWORD 新值 ②同名相似字段跨文件误改 ③#23 纪律案重演 | ④密码以卷内真值为准；改型清单按文件路径逐项核对+编译器哨兵价值实证；交接 Ctrl+S 纪律二次验证
- 2026-11-13 | ④-3 验收全绿：18号白捡首报(P10 总分null)/22号 53.33+20号 7.50 手工复算吻合(J4/J11)/INJ五发三不全过/交卷封卷闸实弹/误触重发撞出 P4 覆盖重评活剧本 | — | #25-28 立案待裁；提交 9717659（含补收 Day10 漏网 LenientScoringConverter——git status 核对拦漏裹）
- 2026-11-12 | ④-2 三连编译错：接口"没有"generateReport 声明、converter "没有"getFormat——但 Read 明明见到完整版 | IDEA 未保存 buffer（新）与磁盘（旧）分叉：Read 见 buffer、mvn/SearchReplace 见磁盘 | 交接 Ctrl+S 纪律（待确认 #23）；与 11-05 旧缓冲案互为镜像，同一课上了两次
- 2026-11-12 | 报告冒烟材料暴露 update_time 比 create_time 早 8h（12:19 vs 20:00） | MySQL 容器 UTC 时区经 `ON UPDATE CURRENT_TIMESTAMP` 写库，MP fill 用 Java 本地时间——同列两时钟 | 立案 #15 待裁；连带翻案 D2 演练 40s 抢单真因（#20），sweep min-idle 至今形同虚设
- 2026-11-10 | 导师脚本二连坑：python heredoc 内半角引号 SyntaxError ×2；且换行分隔导致脚本失败后 git commit 照跑，提交信息先行描述了未发生的 STATUS 修改 | 链式命令未用 && fail-fast；文档修改误用 python 处理中文 | 补正提交落地事实；纪律更新：改中文文档一律 Edit 工具，脚本链必须 &&，提交信息不许预支未发生的事实
- 2026-11-09 | 演练陷阱①：IDEA Stop 按钮在断点挂起时先放行线程——"假死"实锤，D1第一次演练活干完才停 | 停止≠kill -9 | 演练杀进程一律 taskkill //F //PID（AGENTS工具口径已钉）
- 2026-11-09 | 演练陷阱②：DEL 流 key=消息+消费组+PEL 一锅端（组是key附属品，NOGROUP血案与两具孤儿标本） | 演练工具过猛 | 收尾靠 ensureGroup/自愈或 XGROUP CREATE MKSTREAM；运维禁用 DEL，用 XTRIM
- 2026-11-09 | count 事实异步化途中丢失(H1×H6 组合缝) | 同步参数未外化为状态 | R5 表列方案;入宪教训:异步化=把参数变成状态
- 2026-11-09 | 导师 git add 误裹学员工单②文件提交 | 提交前未 git status 核对+通配过宽 | reset --soft 回退保留工作区;规矩:导师只提 docs/STATUS,代码归学员
- 2026-11-04 | 3306 bind 失败 | 本机 MySQL 服务占用 | 停服务后用 Docker 独占 3306
- 2026-11-04 | `^[[200~` 粘贴乱码 | Git Bash 不识别 bracketed paste | 复制命令/关 bracketed paste
- 2026-11-04 | `MYSQL_ DATABASE` 空格 | invalid reference format | 多行命令用复制，读报错定位镜像名异常
- 2026-11-04 | 向导生成 Boot 4.1.2 与 MP 生态不匹配 | 版本选型未看兼容矩阵 | 回落基线 3.5.3；教训已记入版本决策记录
- 2026-11-04 | 404 被 `Exception.class` 兜底吞成 500 | `NoResourceFoundException` 属 Exception 家族；缺更具体的 404 handler | 加专用 handler→code=404+HTTP404；补录"框架码镜像 HTTP"编码规则(文档v0.2)。流程示范：技术窗口上报→契约owner裁决，不私改
- 2026-11-05 | 验收A/B诡异: A报500但库有数据,B重复却返回0 | 三连击: ①电脑重启容器全停(Exited) ②Git Bash curl 发中文=GBK字节→Jackson解析炸→500 ③导师Read到的是编辑器未保存旧缓冲,磁盘才是真相 | docker start 恢复; 中文入UTF-8文件用--data-binary @file; HEX()验证库内数据实为正确"李四"。教训:证据链>猜测,报错先读堆栈找 Caused by
- 2026-11-05 | 协议层JSON解析错误混进500桶 | HttpMessageNotReadableException无专用handler | 裁决#3: code=400镜像规则(文档v0.3),学员补两行handler
