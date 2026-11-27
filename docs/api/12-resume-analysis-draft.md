# 合同 12 草案 · 简历分析

简历分析页已确认采用纵向结构。先展示核心评价和综合分，接着展示五维雷达图，最后按优先级列出问题和原因。上传后的进度条由任务状态驱动。 [已确认的页面结构](../../frontend/prototypes/interview-ui/App.vue)

## 第一段联调

先用一份已有的结构化简历生成真实报告。目标岗位读取该简历的 `targetJob`，这样能沿用现有的简历校验，也能直接把同一个 `resumeId` 带进面试。目前简历表单没有独立的项目经历字段，首个联调用例应使用在自我介绍里填写了项目、个人职责和成果的简历。 [合同 03](03-resume-api.md) · [合同 06](06-interview-session-api.md)

| 接口 | 契约草案 |
| --- | --- |
| `POST /resume/{id}/analyses` | 校验简历归属，记录本次输入，返回 `Result<ResumeAnalysisTaskVO>`，其中包含 `analysisId` 和 `status`。同一简历的同一版本已有进行中任务时，返回该任务。 |
| `GET /resume/analyses/{analysisId}` | 按登录用户读取任务，返回 `status`、`stage` 和关联 `resumeId`。成功时附带 `report`，失败时返回可映射的 `failureCode`。他人的任务按 404 处理。 |

状态从 `QUEUED` 到 `ANALYZING`，再到 `SUCCEEDED` 或 `FAILED`。只有报告已写入才发布 `SUCCEEDED`。前端复用现有轮询机制，使用 `stage` 的 `QUEUED`、`SCORING`、`ASSEMBLING` 和 `COMPLETED` 展示当前进度。阶段内部时长未知时使用动态进度条，报告就绪时完成进度并展示内容。

建议在 MySQL 保存分析任务、用户归属、输入版本和报告结果，复用现有 Redis Stream 异步处理的组织方式。消息携带任务 ID，消费端按任务状态认领并防止重复写入。任务入队失败和消费者超时需要有可见的失败或恢复出口。若使用新增任务表，按项目约定在状态更新中显式维护 `update_time`。

## 报告字段

接口沿用项目的 `Result<T>` 外层。下例为成功响应的 `data`，分数与措辞仅作接口示例。

```json
{
  "analysisId": 51,
  "resumeId": 9,
  "status": "SUCCEEDED",
  "stage": "COMPLETED",
  "report": {
    "verdict": "岗位方向明确，项目证据偏弱",
    "summary": "覆盖岗位的基础技术要求，个人职责和性能结果的验证口径仍需补充。",
    "totalScore": 76,
    "dimensions": [
      { "id": "fit", "score": 85 },
      { "id": "ownership", "score": 72 },
      { "id": "evidence", "score": 66 },
      { "id": "depth", "score": 76 },
      { "id": "writing", "score": 81 }
    ],
    "issues": [
      {
        "priority": "critical",
        "dimensionId": "evidence",
        "title": "延迟降低 40% 缺少验证口径",
        "reason": "简历没有说明延迟指标、优化前后数值与压测条件。",
        "evidenceKbIds": []
      }
    ]
  }
}
```

五维 ID 按原型对应岗位匹配、个人贡献、成果可信度、技术深度、表达结构。每维恰好出现一次，使用 0–100 的整数。建议首版由后端对五项分数等权取平均并四舍五入。问题级别依次为 `critical`（优先修正）、`major`（重要改进）、`polish`（表达优化），同级由影响较大的问题优先。分数反映维度的整体表现，问题级别反映单项问题的影响。

模型评价简历能证明的内容，每条问题带简短标题和具体理由。服务端校验结构和枚举，对输入证据不足的结论明确说明缺了什么。知识库接入时用岗位和相关技能检索审核通过的条目，给相关判断补充依据。检索空手时继续按简历评价，出处数组留空。 [合同 08](08-knowledge-rag-api.md)

## PDF 上传与面试衔接

上传扩展建议用 `POST /resume/analyses` 接收 `multipart/form-data`，字段为 `file` 和 `targetJob`。目标岗位用于岗位匹配维度。接受可提取文字的 PDF，解析期间通过同一查询接口返回 `EXTRACTING` 阶段。扫描件或空文件需要明确的失败状态与重选入口。

从 PDF 提取的结构化字段先给用户确认。用户保存后再用生成的 `resumeId` 建立面试场次。当前 `POST /resume` 成功响应的 `data` 为空，PDF 保存步骤需要在合同里明确怎样返回新 ID。如果新增专用的确认保存入口，建议返回 `{ resumeId }`，同一任务重复确认时返回已经关联的 ID。 [现有简历控制器](../../backend/src/main/java/com/itlzy/aiinterview/controller/ResumeController.java)

## 验收用例

- 选一份已有简历，提交分析后能够查到进行中状态，成功后拿到一份后台实际生成的报告。雷达图五项齐全，问题按级别显示。
- 同一版本重复提交时复用进行中的任务。其他账号查询该任务返回 404。模型调用失败后页面能看到失败状态并重新发起。
- PDF 通路接入后，从真实文件进入进度页并展示真实报告。校验失败和无法提取文字时，用户可以重选文件。
- 报告中的 `resumeId` 可直接用于现有创建面试入口。面试配置仍由用户确认后进入。
