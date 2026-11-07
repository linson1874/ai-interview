# 接口文档 — 简历模块 v1.2（正式版）

> 学员起草五个决策点 → 导师评审放行 v1.0 → 学员复审抓循环引用 v1.1 → Day6 实现评审+三项裁决升 **v1.2**（生效版）。
> 通用约定（Result 包装、错误码规则、鉴权）继承 01/02 文档。

## 2.1 校验提示语定稿表（v1.1 裁决补录，修复"见4/见2"循环引用；实现与测试以此为准）

| 字段 | 违规 | 提示语 |
|---|---|---|
| realName | 空 | 真实姓名不能为空 |
| realName | 长度越界 | 真实姓名长度需在2-20个字符之间 |
| realName | 白名单外字符 | 真实姓名仅支持中英文、空格与"·" |
| targetJob | 空 | 目标岗位不能为空 |
| targetJob | 长度越界 | 目标岗位名称过短或过长 |
| education | 超长 | 学历描述不能超过64个字符 |
| skills | 空数组/全空白 | 请至少填写一项技能 |
| skills | 条数>10 | 最多只能添加10项技能 |
| skills | 单条>20字 | 单个技能名称不能超过20个字 |
| selfIntro | <10（非null时） | 自我介绍不能少于10个字 |
| selfIntro | >1000 | 自我介绍不能超过1000个字 |
| size | 越界(<1 或 >50) | 每页条数需在1-50之间 |
| page | <1 | 页码不能小于1 |

> 分页两则为裁决②批准学员暂拟稿入表定稿；分页参数校验允许在 Service 层手工判（query 参数不走 @Valid）。
> **成功 msg 统一为 "ok"**（继承 01 §1；"简历保存成功"之类业务文案归前端拥有——文案权在展示层）。

实现备注（裁决#7批准）：`selfIntro` 的上下限用 **`@Size` 可重复注解拆两条**各配各的 message（Bean Validation 2.0+，validation starter 原生支持）；同字段多规则违规时全局 handler 取第一条 FieldError，顺序不作承诺。skills 的空白标签**先 trim 后判空**，trim 后全空按"空数组"处理。

## 1. 总则

- 路径统一 `/resume/**`，全部纳入登录拦截（`WebMvcConfig` 给 `/resume/**` 加 `addPathPatterns`，无免检项）
- 软删除语义：`deleted=1` 后一切读接口都当它不存在（@TableLogic 自动兜底）
- 分页参数：`page` 从 1 起；`size` 默认 10，**上限 50**（MP 分页插件无默认上限，`maxLimit` 必须显式配置——不配就是裸奔）

### 错误码补充说明（v1.0 裁决，纳入 01 文档码表的适用范围）

| code | 本模块内新增语义 |
|---|---|
| 404 | 同时覆盖"路由不存在"与"**目标资源不存在/不属于当前用户/已删除**"，msg 统一为"资源不存在或已删除"（防资源枚举；不区分具体原因，不给探测者留信息） |

## 2. 数据字段

| 字段 | 对应列 | 规则（v1.0 定稿） |
|---|---|---|
| realName | real_name VARCHAR(32) | 必填；2-20 个字符；白名单：中英文、空格、"·"（民族/外籍姓名兼容，**不得过严拒真实姓名**）；提示语见 §2.1 |
| targetJob | target_job VARCHAR(64) | 必填；2-50 字符；"目标岗位名称过短或过长" |
| education | education VARCHAR(64) | 选填；≤64 |
| skills | skills VARCHAR(1024) | 必填；JSON 字符串数组存库；**≤10 条、单条≤20 字符、去空白**；接口出入参为数组，序列化/解析在服务层完成；提示语见 §2.1 |
| selfIntro | self_intro TEXT | 选填；**允许为 null；若提供则 10-1000 字符**（可选≠空字符串也合法的语义歧义已封死） |

## 3. 接口清单

| 接口 | 方法/路径 | 语义 |
|---|---|---|
| 新建简历 | POST /resume | 为当前登录用户创建（userId 取自凭证，见 D1） |
| 修改简历 | PUT /resume/{id} | 仅当 id 属于当前用户才生效；否则按"资源不存在"（D1b，404）。**全量替换语义（D6）：请求体未携带的可空字段=清空** |
| 删除简历 | DELETE /resume/{id} | 逻辑删；**不论 id 归属/存在/已删，一律 code=0**（幂等+防枚举，越权也静默成功——见 D7） |
| 分页列表 | GET /resume/page?page=&size= | 只回自己的；size 越界报错（D2）；**排序：createTime 降序 + id 降序**（id 次键保证翻页稳定，裁决③） |
| 查看详情 | GET /resume/{id} | 不存在/非本人/已删 → 统一 404（D5） |

## 4. 决策记录（学员拍板 + 导师修订）

- **D1a（学员答，原文保留）**：userId 从登录凭证取。请求参数/body 传 userId 均可被篡改→水平越权。✅
- **D1b**：~~学员答 401~~ → **修订为 HTTP 404 + code=404，msg"资源不存在或已删除"**。理由：请求者票是真的（已过拦截器），不是"未认证"；与 D5 防枚举策略统一，且实现上"按 userId 限定后查询为空"与"不存在"同一条代码路径。⚠️ 评审发现其与学员 D5 答案自相矛盾，已按 D5 精神修正——用例互洽是合同评审的本分。
- **D2**：size 上限 **50**（~~学员记的"MP 默认 500"为讹传：MP 无默认上限，需显式 maxLimit~~）；越界**报错 code=2**（学员选，理由采纳：静默钳制会让前端误判拿到全量数据，API 不撒谎）。
- **D3**：定稿见第 2 节规则 + §2.1 提示语表（v1.1 修复循环引用：v1.0 曾"见4/见2"互指，文案实未落地——学员复审发现，裁决#7 补录）。两处评审修订：姓名规则放宽防误伤；selfIntro"选填+长度"的 null 语义封死。
- **D4**：列表项 = id/realName/targetJob/skills(数组)/createTime。**删除学员虚构的 status 字段（t_resume 无此列——合同字段必须可溯源）**。不含 selfIntro（取舍理由：列表只需要识别度高的摘要字段；全文只在详情有意义）。

```json
{ "code": 0, "msg": "ok", "data": {
    "list": [ { "id": 101, "realName": "张三", "targetJob": "Java后端开发",
                "skills": ["Java","Spring Boot","MySQL"],
                "createTime": "2026-11-06 10:00:00" } ],
    "total": 45, "page": 1, "size": 10
} }
```

- **D5（学员思路保留）**："查不到"三种成因（不存在/非本人/已删）**统一返回**，防资源枚举，不回 403。✅ 定稿：HTTP 404 + code=404。
- **D6（v1.2，裁决①）PUT 全量替换**：理由——编辑页是整表单提交，"用户把自我介绍删空再保存"必须是能生效的真实需求；增量语义下该操作静默失败。实现红线：**MP `updateById` 默认跳过 null 字段**（Day6 实测复现：省略 education 后旧值 PhD 保留），故 `Resume` 实体的 `education/selfIntro` 两字段标 `@TableField(updateStrategy = FieldStrategy.IGNORED)` 显式允许 null 落库（本实体唯一更新路径即全量替换，无副作用）。
- **D7（v1.2，评审发现）DELETE 防枚举收口**：v1.1 实现"删他人活跃简历→404 / 删不存在→code0"会向探测者泄露"id 上是否有活人资源"，违背学员自己的 D5 原则。定稿：**一条条件删除** `delete(wrapper.eq(id).eq(userId))`，影响行数不论多少一律 code=0——归属判断、存在性、幂等三件事被一条 SQL 原子做完，双查+竞态全删。

## 5. 验收用例（期望值由 D1/D2/D5 推出，导师代填——这列本来就是决策的推论，决策对了它自己会填）

| # | 场景 | 期望 |
|---|---|---|
| R1 | 新建合法简历 | code=0，库中多一行 user_id=当前用户 |
| R2 | selfIntro 传 1001 字 | code=2，"自我介绍不能超过1000个字"，库无新增 |
| R2b | selfIntro 不传(null) | **code=0**（选填语义验证） |
| R3 | PUT 他人简历 id | HTTP 404 + code=404，"资源不存在或已删除"，且对方数据未被改动 |
| R4 | size=99999 | code=2，提示 size 越界；**不是**钳制后 200 |
| R5 | 列表含他人/已删简历？ | code=0 且 list 内无 user_id≠本人、无 deleted=1 的行 |
| R6 | DELETE 成功后再 GET 详情 | HTTP 404 + code=404（同 D5 路径）；对同 id 再 DELETE 幂等 code=0 |
| R6b | DELETE **他人活跃**简历的 id | code=0（D7：一律幂等静默，不得区分归属——v1.1 实现返回 404 属枚举侧信道，已修订） |
| R8 | PUT 省略 education → GET 详情 | education=null（D6 全量替换生效判据；v1.1 实测省略后旧值 PhD 保留=未达标） |
| R8b | selfIntro 恰好 1000 字 | code=0（边界含端点，已实测通过） |
| R7 | 未带 token 调任一接口 | HTTP 401 + code=3（继承合同02） |

## 6. 实现清单（放行后可动工）

1. `entity/Resume` + `mapper/ResumeMapper`（照 User 手感，练自己敲）
2. `config/MybatisPlusConfig`：注册 `MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.MYSQL)` 并 **setMaxLimit(50)**（合同 1 的依据就在这）
3. `dto/ResumeSaveDTO`（新建/修改共用一个，校验全在第 2 节）
4. `vo/ResumeListItemVO`（五字段）/ `vo/ResumeDetailVO`（含 education/selfIntro）——**两个 VO 形状不同，正是 D4 的落地**
5. Service：所有查询/更新条件都带 `user_id = 凭证userId`（越权天然查不到→统一 404 的代码路径）；skills 字符串↔数组转换
6. Controller：REST 风格五端点；`WebMvcConfig` 把 `/resume/**` 纳入拦截
7. 验收：跑 R1~R8b（幂等与边界别漏）

## 6.5 Day6b 修复清单（v1.2 裁决落地，完成后阶段一收官）
1. `Resume` 实体：`education/selfIntro` 两字段加 `@TableField(updateStrategy = FieldStrategy.IGNORED)`（D6 全量替换的 MP 落地开关）
2. `ResumeServiceImpl.delete`：删掉双查询，替换为一条条件删除 `resumeMapper.delete(lambdaQuery().eq(id).eq(userId))` 后直接返回 code=0（D7，顺带消除竞态窗口）
3. Controller 三处成功 msg 改 `Result.ok("ok")`（§2.1 新约定）
4. 清理 `ResumeServiceImpl` 里未使用的 import `java.sql.ResultSetMetaData`（IDEA 补全事故，代码卫生）
5. 回归 R1~R8b 全绿
