# Findings

## Repository

- 当前工作目录为 `/Users/sakura/Code/X-AgentStudy`。
- 目录初始无业务代码和文档，适合先建立开发文档体系。
- `git status` 显示父级目录存在较多未跟踪/变更内容，本次只在当前项目目录下新增文件。

## Product Core

- 产品核心闭环是：学习方向 -> 画像 Agent -> 规划 Agent -> 计划级知识库 -> 答疑 Agent -> 测验 Agent -> 复习 Agent -> 总结 Agent。
- 用户明确希望不做 MVP，而是按完整 SaaS 版本做开发准备。
- 数据隔离最关键的领域边界是学习计划，知识库、问答、题库、复习、报告均必须挂载到具体计划。

## Engineering Implications

- 后端需要按业务模块拆分服务，并单独保留 Agent 协调层。
- AI 调用层需要模型供应商抽象，避免业务直接耦合 OpenAI、DeepSeek、百炼等厂商。
- 文档解析、摘要、向量化、题目生成属于异步任务，需要任务状态模型和前端轮询/推送机制。
- 前端需要覆盖完整 SaaS 导航与能力，但交付仍应按风险拆分阶段推进。
