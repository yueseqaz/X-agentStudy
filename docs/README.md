# X-AgentStudy 开发文档索引

本目录用于沉淀多 Agent 协作技能学习 SaaS 平台的开发前准备材料。文档按工程落地视角组织，后续需求、架构、接口、数据模型和迭代计划应优先在这里维护。

## 文档结构

| 目录 | 用途 |
| --- | --- |
| `product/` | 完整产品范围、用户流程、产品规则 |
| `architecture/` | 系统架构、领域模型、模块边界 |
| `backend/` | 后端服务、API、数据库设计 |
| `frontend/` | 前端页面、路由、交互状态 |
| `ai/` | Agent 设计、RAG、文档处理流水线 |
| `delivery/` | 版本路线图、开发检查清单 |
| `ops/` | 非功能、安全、性能、部署关注点 |

## 新增交付文档（2026-04-23）

1. `delivery/project-implementation-report.md`  
   当前版本已实现能力、技术栈映射、可演示链路、边界说明。
2. `ops/deployment-guide.md`  
   Docker 部署与他人电脑本地部署的完整步骤。
3. `delivery/ppt-generation-prompt.md`  
   可直接交给其它大模型生成项目 PPT 的高质量提示词模板。

## 推荐阅读顺序

1. `product/product-scope.md`
2. `architecture/system-architecture.md`
3. `architecture/domain-model.md`
4. `backend/database-design.md`
5. `backend/api-design.md`
6. `ai/agent-design.md`
7. `frontend/page-spec.md`
8. `delivery/roadmap.md`

## 产品主链路

用户创建学习方向 -> 画像 Agent 采集信息 -> 生成学习画像 -> 规划 Agent 生成学习计划 -> 上传资料并构建计划级知识库 -> 计划内 AI 问答 -> 生成测验并沉淀题库 -> 复习与学习报告。

## 技术基线

- 后端：Java、Spring Boot、Spring MVC、MySQL、Redis、Spring Security/JWT。
- 前端：Vue 3、Vite、TypeScript、Vue Router、Pinia、Axios、Element Plus。
- AI：Spring AI、模型网关、自定义 Agent 编排、计划级 RAG、向量数据库。
