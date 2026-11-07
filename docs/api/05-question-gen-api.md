# 接口文档草案 — AI 出题模块 v0.1-DRAFT（决策点🔶由学员拍板，导师评审放行后实现）

> 通用约定继承 01/04。鉴权：`/ai/**` 已在拦截区。本版**同步生成、只回显不落库**——落库与异步化是阶段三 Redis Stream 的改造对象，先让功能对，再让功能快。
> 产品上下文：拍板前先读 `docs/product/ai-feature-map.md`（本接口在其地图第 1 行；Q1b/Q3 的推荐依据见其 §4）。

## 1. 背景与本版范围

用户已有结构化简历（合同 03）。本接口把简历喂给大模型，换回一批个性化面试题。**它是整个项目"AI 含量"的第一块真肌肉**：prompt 工程 + 结构化输出 + 失败策略。

范围外（留档阶段三）：生成结果写入 `t_interview`/`t_interview_question`、异步任务、面试会话。

## 2. 接口：生成面试题 `POST /ai/questions`（形状细节由 🔶 决定）

### 🔶Q1 输入面
a) 入参用 `resumeId`（路径/query/body 你定）——它必须属于当前凭证用户（404 语义直接沿用合同 03 D5，不许重新发明）。
b) 开放题：**没填简历的用户能不能出题？**（提示：裁决时你亲口定过 `t_interview.resume_id` 可空留了口子。若"能"，输入变成什么？若"不能"，错误语义用什么码、什么文案？二选一写清楚，不许骑墙。）

### 🔶Q2 题数与成本
题数从哪来（固定值？入参？）上限多少？写出你的依据：单题大约消耗多少 prompt token（简历字段+指令）、一题输出多少 completion token（用你上次实测 usage 的倍数估），算出"count=上限时一次请求的成本量级"。**我要看到算术，不是感觉。**

### 🔶Q3 输出 schema
JSON 数组每题字段建议：`category`（八股/项目/场景/HR——对齐 t_interview_question 的枚举！去把表里那句 COMMENT 抄来）、`difficulty`（1-3）、`question`（题干）。
开放题：**要不要带 `refAnswer`（参考答案）？** 从三个角度各写一句：成本/延迟、用户价值、阶段三评分时有没有用。拍板"要/不要"。

### 🔶Q4 格式失败策略（本课的灵魂）
模型返回不守 JSON 规矩（裹 ```json 代码块、加解释文字、字段名拼歪）时：
a) 直接失败 code=500 类？还是**重试一次**？重试的提示词怎么改？
b) 重试仍失败，对外文案写什么（参考合同 04 的"不透露上游细节"纪律）？
c) 实现路线自己调研一句：Spring AI 的 `BeanOutputConverter<T>` 是怎么"用 Java 类型反向生成格式要求"的——写出它进 prompt 的是**原文**还是**模板**（提示：去 javadoc/文档里找 `getFormat()`）。

### 🔶Q5 system / user 分工
给出一段你写的 system prompt（角色、只出题不闲聊、字段约束、禁止输出解释文字）与 user prompt 的分工说明（简历字段怎么拼进去、**skills 数组拼成什么形态**——原始 JSON 串还是人类可读顿号分隔？模型吃什么更听话？写一句你的选择与理由）。这段会被逐字评审。

## 3. 响应草案（成功形态，细节随 Q3 定稿）

```json
{ "code": 0, "msg": "ok", "data": {
    "questions": [ { "category": "八股", "difficulty": 2, "question": "……" } ],
    "model": "deepseek-flash",
    "usage": { "promptTokens": 0, "completionTokens": 0 }
} }
```

（usage 回显从第一天就带上——成本可观测性是 AI 应用工程师的习惯，也是你 S1 那次抓出模型漂移的眼睛的续作。model 字段沿用合同 04 的"应答方报什么显什么"。）

## 4. 验收用例（期望值随你的 🔶 答案推导，互洽检查照旧）

| # | 场景 | 期望 |
|---|---|---|
| G1 | 带合法本人 resumeId 出题 | code=0，题数=Q2 定稿，字段=Q3 定稿，usage 非零 |
| G2 | resumeId 是别人的 | ____（沿 03 的哪条？） |
| G3 | count 越界（若 Q2 允许入参） | code=2 文案=____ |
| G4 | （按 Q1b 你的决定补一条对应的场景与期望） | ____ |
| G5 | 构造畸形 JSON（临时把 system prompt 里"禁止解释"删掉诱发） | ____（按 Q4 策略推） |
| G6 | 无 token | HTTP 401 + code=3 |

## 5. 放行条件

Q1~Q5 全拍板 + 用例期望互洽 + Q5 的 system prompt 文案交我**逐字评审** → v1.0 → 实现清单（DTO/VO/Converter/Service/Controller + `@Disabled` 付费测试一条 + 免费路径 mock 测试思考一条）。
