# 接口文档 — AI 问答模块 v0.1（Day7 最小闭环）

> 合同性质同前。通用约定继承 01 v0.3（Result 包装、错误码规则）。鉴权：`/ai/**` 纳入登录拦截。

## 0. 供应商架构决策（裁决#9）

- 主力：**阿里云百炼 OpenAI 兼容端点**（`base-url: https://dashscope.aliyuncs.com/compatible-mode`，starter 用 Spring AI openai）
- 原则：**只依赖 OpenAI 兼容协议，不绑定厂商**；模型/供应商切换 = 配置变更（qwen-plus 起步；百炼模型广场内的 deepseek-v3 / glm 系列列为后续可选项，模型 ID 以控制台实测为准）
- key 走环境变量 `AI_API_KEY`，不进 yml 明文、不进 Git（老红线）
- 成本护栏（第一档）：登录保护 + `max-tokens: 512`；限流与用量记账留待阶段三前裁决

## 1. 接口：AI 问答 `POST /ai/chat`

### 请求

| 字段 | 类型 | 必填 | 校验 | 提示语 |
|---|---|---|---|---|
| question | string | 是 | @NotBlank；≤500字 | "问题不能为空" / "问题过长(≤500字)" |

### 响应（成功，HTTP 200 + code 0）

```json
{ "code": 0, "msg": "ok", "data": { "answer": "索引相当于书的目录……", "model": "qwen-plus" } }
```

（model 字段回显实际模型名：为多供应商切换的可观测性从第一课就埋好）

### 失败语义

| 场景 | HTTP | code | msg |
|---|---|---|---|
| 参数违规 | 200 | 2 | §2.1 文案 |
| 未登录/凭证失效 | 401 | 3 | 继承合同02 |
| **上游 AI 异常**（key 失效/额度耗尽/超时/网络） | 200 | 500 | **"AI 服务暂时不可用，请稍后重试"**——注意：不得把 SpringAiException/HTTP 401-上游 的细节透传给前端（信息泄露 + 前端无法区分"系统挂了"和"额度没了"是运维的事，不是用户的） |

实现约束：上游异常须被 GlobalExceptionHandler 捕获并 `log.error` 全量堆栈（对外简答，对内可查——Day5 兜底纪律的延续）。

## 2. 验收用例

| # | 场景 | 期望 |
|---|---|---|
| S1 | 带 token 问"一句话解释什么是索引" | code=0，answer 非空，model 与 yml 配置一致；报告 usage token 数 |
| S2 | question 为空串 | code=2 |
| S3 | question 501 字 | code=2 |
| S4 | 不带 token | HTTP 401 + code=3 |
| S5 | （可选）临时把 key 改错一位再问 | **对外仍是 500+统一文案**，日志里能看到真实上游错误；改回 key |

## 3. 预告（不在本版范围）

- 流式输出（SSE/stream）——阶段三随异步出题一并引入
- 简历→面试题生成（结构化输出）——Day8 主课
- RAG/向量检索——阶段四，届时百炼也可能只当"生成端"，检索端交给 pgvector
