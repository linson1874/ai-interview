# 问题档案（支持窗口答疑索引）

> 由技术支持窗口维护：每次答疑后按日追加一节，供复习回看。
> 定位是"索引 + 一句话答案"，不是教材——细节没记住就按主题去翻对应代码/合同文档，或回支持窗口重问。

## 2026-11-19

### 代码解读类（阶段一~四已写代码）
| # | 问题 | 一句话答案 | 相关代码 |
|---|---|---|---|
| 1 | GlobalExceptionHandler 拦截逻辑 | @RestControllerAdvice 收全项目异常；按"类型最具体优先"分发到三个 handler（校验失败→2、唯一键冲突→1、兜底→500+记日志）；命中后 HTTP 仍是 200，业务成败看 code | common/GlobalExceptionHandler |
| 2 | JwtUtil 解读 | 发票=createToken(subject=userId+过期+签名)；验票=parseUserId 验签和读票同一步，假票读不出；secret 必须≥32字符；票面是编码不是加密，不放敏感信息 | util/JwtUtil |
| 3 | DTO/VO 方向 | DTO=前端→后端入参+校验注解的家；VO=后端→前端出参、脱敏用；Entity 不上不下只跟数据库 | dto/ vo/ |
| 4 | @NotBlank 和 @Value | @NotBlank=字符串必填(拦null/空串/纯空格)标字段上；@Value=从yml读值灌字段，类必须归Spring管；坑：报"Could not resolve placeholder"=yml没配 | — |
| 5 | AuthServiceImpl.login 解读 | 四步：查人→matches(明文,密文)校验(BCrypt随机盐不能加密后比对)→签JWT+Redis存底单→返回token+nickname；"用户名或密码错误"统一文案防枚举 | service/impl/AuthServiceImpl |
| 6 | LoginInterceptor 解读 | 门禁四步：掏Bearer头→验票面(JwtException)→查Redis底单(注销/互踢在这拦)→setAttribute别工牌；reject手写401+JSON因为不能走返回200的全局handler；Boolean.TRUE.equals防null | config/LoginInterceptor |
| 7 | logout 删的是哪的 token | JWT 删不掉（服务器不留副本），删的是 Redis 底单；效果=票作废；Redis key 有 TTL 不堆积；前端本地那串只是缓存 | — |
| 8 | 注销后重新登录是同一个 token 吗 | 不是——issuedAt 毫秒级不同→票必然不同；新登录新票新底单；旧票双重报废(底单已删+TTL到自蒸发)；多设备多次登录=多票并存，logout 只踢当前票 | — |
| 9 | LenientQuestionsConverter 解读 | 官方BeanOutputConverter是严格派，本类解析前洗表皮脏：剥```围栏→没围栏截首{尾}→交delegate真解析；只救礼貌问题，内容错 Loudly上抛；getFormat照样委托官方生成 | util/LenientQuestionsConverter |
| 10 | 这转换器用到什么技术栈 | Spring AI 结构化输出 + 正则 + 委托组合 + fail-fast不吞错 | 同上 |
| 11 | @Valid 是什么 | 参数前的校验开关；没有它字段上的@NotBlank全是摆设；不过就抛MethodArgumentNotValidException；嵌套对象需级联@Valid；@Validated是Spring加强版(分组) | controller |
| 12 | entity/Mapper 求代码 | 模板作业自己敲；给了DDL→Java对照表：驼峰自动映射不用写@TableField、这表无deleted列不加@TableLogic、两个fill时间照抄 | entity/InterviewQuestion |

### 架构与测试类（阶段三 Redis Stream）
| # | 问题 | 一句话答案 | 出处 |
|---|---|---|---|
| 13 | 两道自动防线是什么 | ①outbox补投(每60s扫status=0且60s没动的行重XADD，不碰状态，status=4免疫)；②消费者撞NOGROUP错误→建组重建；分工:①管"消息丢了"②管"组没了"，②建组依赖①先把key建出来 | InterviewServiceImpl:228 / InterviewGenConsumer |
| 14 | 演练两幕验证什么 | 幕一验补投的边界感(正在生成中的行不许碰，沉默=正证)；幕二验进程被硬杀后靠哪条路恢复(启动清PEL≈秒级 vs sweep 600s)，用实测时间戳终审"40秒案"；必须taskkill //F因为优雅停止会交清PEL毁掉实验前提 | 演练脚本 |
| 15 | 三道防线总结 | 恢复模式(主,3s,领了活人死了自己捡回)+outbox(60s,活没进队列重投)+NOGROUP处理(150ms,组没了重建)+sweep(备用,别人领了活死了)；各堵一个断点互不重叠 | 合同06 |

### Apifox 实操类
| # | 问题 | 一句话答案 |
|---|---|---|
| 16 | 接口怎么分类 | 5文件夹20接口：用户认证4/简历5/面试7/AI2/hello；缺的12个照代码补；接口建一次、场景用"用例"，别再新建接口 |
| 17 | token 2小时过期每次手填太累 | 登录接口后置脚本"验货后写入"：pm.environment.set("token",...)；过期=重发登录自动续；失败响应不污染变量 |
| 18 | 401 Bearer undefined 排查 | 诊断链：实际请求tab看头的真值→"undefined"=变量存在但值是脏字符串→手动贴新token验证链路→根因=提取器没运行时生效，换自定义脚本(带console.log可查控制台) |

### RAG 学习路线（合同08 七个K）
| # | 问题 | 一句话答案 |
|---|---|---|
| 19 | RAG 是什么 | 答题前先翻资料：问题→embedding向量→pgvector找最近topK→资料塞进prompt→模型据资料答+给出处；对比微调：知识常变要溯源用RAG |
| 20 | K2 双数据源 | Boot"你插手我退出"(@ConditionalOnMissingBean)；两个DataSource Bean必须@Primary定正宫给MP/事务；选方案a手工双Bean(b藏生命周期坑,c动态切换=过度设计)；配置前缀pg.datasource分开 |
| 21 | K2 思考题：跨库事务要吗 | 不需要——逐流程摸过：没有任何请求同时写两库(PG业务路径只读,写PG只有离线脚本)；答"要"先去啃分布式事务=三天白干 |
| 22 | 全换PG是不是更好 | 不：双库是业界标准架构(业务库+向量层)；简历亮点是pgvector/RAG不是PG本身；全迁=省K2一堵墙付一周重刷schema+回归网重跑+丢选型故事；第0天规划可,现在不划算 |
| 23 | K4 托管vs自建/审核闸/索引 | 推荐自建(status要CHECK强类型,metadata JSON袋撑不起安全语义)+检索SQL写死approved常量+出口收敛单类(V2硬约束)+本期不建索引(规模小,阈值后HNSW+cosine_ops)；防投毒三环:入库状态机+检索硬过滤+出处可审计 |
| 24 | K3 embedding | 文本→1024个小数,batch入库省往返；维度与表列vector(N)锁死+embedding_model列防混存(V3)；查询入库必须同模型→两key并存(BAILIAN/DEEPSEEK)；成本要算账:题均字数×条数×牌价,牌价截图存档标日期 |
| 25 | K6 接哪端 | 推荐d独立/ai/kb/ask：05/07零升版、旧回归零重跑、当检索调参试验场；动a/b必须带合同升版草案+用例变红归因方案；空检索分支三选一(硬编最坏/拒答/降级标注) |
| 26 | K1 端口/TZ；K7 权限面 | 5433:旧interview-postgres占了5432的登记(端口阴影),错开=买断手滑雷;容器内仍是5432;TZ=Asia/Shanghai新容器主动带(qid61/8h位移教训移植);K7:无HTTP面=无攻击面,403不存在于码表,引入角色=01先升版;能用流程解决的权限不用代码解决 |
| 27 | 五层防线是什么 | ①角色隔离句+②定界符(打包用,概率防)③长度护栏(防刷屏挤出)④硬闸门(score必0~5否则永不入库)⑤最小权限(无工具无联网,收益焊死为改一个数字)；①-③劝、④⑤物理限制 |
| 28 | 题池被投毒五层谁兜得住 | ⑤物理兜底(模型根本拿不到密码,prompt里没有);①②可平移但只减伤(间接注入无解=序言);③无效(短payload);④空档(自由文本无区间校验)=真缺口；新招:输出指纹断言+入库机审扫描+evidenceKbIds秒级召回retired+评测网埋INJ-6带毒题常态化 |

### 合同08 债③四问定稿（导师验收用，2026-11-19 批卷通过）
| # | 问题 | 定稿答案 | 相关代码 |
|---|---|---|---|
| 29 | active:dev 删了会怎样 | 启动失败——dev.yml 是 datasource/redis/jwt/MP/pg/ai 六段的唯一载体，删一行=六处断供；报错词已换代：不是自动配置时代的 Failed to configure DataSource，是自家 DualDataSourceConfig 在 url 空时 build() 抛异常（我们把自动配置逼退位了，报错也跟着换成我们的）；MP 逻辑删除配置丢失的具体后果：deleted 条件不再拼接，已删数据复活、D7 幂等删除语义作废；profile 机制的所以然：敏感配置进本地进 gitignore，进 Git 的只有中性骨架 | application.yml / DualDataSourceConfig |
| 30 | UserMapper.insert 哪来的 | 继承 BaseMapper<User> 白得内置 CRUD；三层机件：启动期 TableInfoHelper 解析实体元数据(@TableName/@TableId)→SqlInjector 预注册 MappedStatement(含 insert SQL)→运行期 MapperProxy(JDK动态代理，接口无实现类)路由到预生成语句执行；一句话记忆：**解析一次、注入一次、路由每次**——SQL 不是调用时现拼的（面试追问"每次都反射？"就靠这句接） | mapper/UserMapper |
| 31 | createTime 谁填的 | 三步链：实体字段 @TableField(fill=INSERT) 打标记 → config/MyMetaObjectHandler(@Component) 的 insertFill 里 strictInsertFill 只在 null 时补 LocalDateTime.now() → MP 执行 SQL 前回调它；王牌故事：create_time 走 MP=Java 的钟，update_time 曾被表上 ON UPDATE 走 MySQL 容器的钟(UTC)，同列两时钟差 8h=#15 时区大案，清扫条件形同虚设的真因，TZ=Asia/Shanghai 一处根治——机制谁填的不重要，**哪个钟填的**才致命 | config/MyMetaObjectHandler |
| 32 | compose 三问（旧教程项目） | docker ps -a 现场取证：旧项目 **3 个容器**——interview-postgres(镜像 pgvector/pgvector:pg16！教程本来就是向量库)、interview-redis、interview-rustfs(9000/9001=S3 API+控制台标准牌)，**无 MySQL 无后端容器**；rustfs 存简历 PDF 类文件：二进制大对象塞关系库=备份膨胀/查询拖慢/没法流式下载，文件归对象存储、库里只存 key；"为何还留 MySQL"双层答：找相似(pgvector)与管交易(业务库)两职分工，与我们双 DataSource 同一架构判断；我们 Docker 起 MySQL=版本/参数/端口由 compose 说了算、换机 clone 即复现、随起随停不污染本机——但环境统一连坑也统一：容器默认 UTC 引出 #15 案，TZ 参数是学费换来的 | docker ps -a / AGENTS基线 |

---
（后续日期由支持窗口追加）
