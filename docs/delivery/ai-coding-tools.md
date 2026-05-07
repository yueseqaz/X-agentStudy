# AI Coding 工具说明

## 使用范围

本项目开发过程中使用 AI Coding 工具辅助完成需求拆解、代码生成、文档整理、问题排查和测试建议。AI 只作为协作工具，最终代码、功能边界、测试结果和提交内容由团队确认。

## 工具清单

| 工具 | 用途 | 协作要求 |
|---|---|---|
| Codex | 代码阅读、局部实现、文档补齐、测试命令执行 | 不提交真实密钥；不覆盖人工已有改动；所有改动需经过本地验证 |
| ChatGPT / 大模型对话工具 | 方案梳理、PPT 文案、答辩内容组织 | 输出仅作草稿，需结合项目实际能力修订 |
| GitHub Copilot / 同类补全工具 | 局部代码补全、重复结构生成 | 生成内容需人工检查命名、边界条件和授权风险 |

## 代码位置标注

项目中 AI 相关能力主要集中在：

- `backend/src/main/java/com/xagentstudy/agent/orchestration/`
- `backend/src/main/resources/prompts/`
- `backend/src/main/java/com/xagentstudy/qa/`
- `backend/src/main/java/com/xagentstudy/quiz/`
- `backend/src/main/java/com/xagentstudy/knowledge/`
- `backend/src/main/java/com/xagentstudy/video/`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/PlanView.vue`
- `frontend/src/views/KnowledgeView.vue`

## 合规说明

- 仓库不提交真实 API Key、数据库密码和个人隐私数据。
- `.env.example` 只保留示例变量。
- 模型配置由管理员在系统内维护。
- AI 生成内容进入系统后仍需要经过结构化解析、展示边界和用户确认。

