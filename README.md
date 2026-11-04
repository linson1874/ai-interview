# AI 面试系统（ai-interview）

面向秋招的全栈 AI 应用练习项目：基于简历异步生成面试题、AI 评分、八股文 RAG 知识库问答。

## 技术栈
Spring Boot 3.5 / MyBatis-Plus / MySQL 8 (Docker) / Redis 7 (Docker, Stream 异步任务) / pgvector (RAG)

## 仓库结构
```
ai-interview/
├── sql/            # 数据库脚本,按编号增量维护(幂等)
├── backend/        # Spring Boot 代码模块
├── AGENTS.md       # AI 协作约定与设计基线
└── STATUS.md       # 项目进度账本
```

## 快速启动
```bash
# 1. 起基础设施
docker start ai-mysql ai-redis    # 首次:见 sql/ 与 README 历史说明
# 2. 配 backend/src/main/resources/application-dev.yml (数据库账号密码,不进Git)
# 3. 启动: IDEA 运行 AiInterviewApplication (推荐) 或 mvn spring-boot:run
# 4. 验证
curl http://localhost:8080/hello
```

## 里程碑
- [x] 阶段一：数据库设计 + 工程骨架 + 用户体系（进行中）
- [ ] 阶段二：Spring AI 接入大模型
- [ ] 阶段三：Redis Stream 异步出题
- [ ] 阶段四：pgvector RAG 知识库
