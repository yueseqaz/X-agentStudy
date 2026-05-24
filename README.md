# X-AgentStudy

X-AgentStudy 是一个面向技能学习的多 Agent 协作平台。项目围绕“从学习目标到学习结果”的完整流程设计：用户输入学习方向，系统生成学习画像和学习计划，再通过知识库、AI 问答、题库、复习、报告、资源推荐和社区讨论持续辅助学习。

项目适合作为大学生软件设计大赛、课程设计或 AI 教育产品原型展示，重点体现 Agent 协作、个性化学习路径、知识库增强问答和学习闭环。

## 项目亮点

- 个性化学习画像：根据用户目标、基础、时间预算和偏好生成学习画像。
- 智能学习计划：自动生成章节、单元、知识点、任务和阶段目标。
- 计划内 AI 问答：回答会强关联当前学习计划；即使没有上传资料，也能围绕计划主题回答。
- 知识库增强：支持上传资料、解析切片、召回引用，并用于问答、文档生成和题库生成。
- 题库与复习闭环：支持客观题、主观题、AI 批改、错题复习和阶段检测。
- 学习报告：统计正确率、任务完成率、薄弱点、周复盘和下一步建议。
- 问答社区：用户可以发布问题、回答问题，也可以邀请 AI 在评论区回答；问题可关联学习计划。
- 计划分享：学习计划可以通过链接和二维码分享，其他用户打开后可以应用到自己的账号。
- 管理后台：支持用户、额度、模型配置、资源库和任务状态管理。

## 主要功能

| 模块 | 说明 |
|---|---|
| 用户与权限 | 注册登录、Token 鉴权、角色权限、账号安全 |
| 学习方向 | 创建学习方向，承载后续画像、计划和学习记录 |
| 学习画像 | 分析用户基础、目标、风险和学习策略 |
| 学习计划 | 生成结构化学习计划，支持任务进度、计划调整和分享 |
| 知识库 | 上传资料、解析文档、生成知识切片和知识图谱 |
| AI 问答 | 基于计划上下文和知识库回答问题，支持 Markdown 展示和打字机效果 |
| 题库测验 | 按知识点生成题目，记录作答和 AI 反馈 |
| 复习报告 | 输出学习报告、薄弱点、周复盘和阶段检测 |
| 资源库 | 管理和推荐学习资料、课程、文档 |
| 问答社区 | 卡片式问题列表、详情回答区、@AI 回答 |
| 管理后台 | 模型网关、额度、资源、用户和任务管理 |

## 技术栈

后端：

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Redis
- Flyway
- Spring AI
- Maven

前端：

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Element Plus

AI 能力：

- Spring AI 模型调用网关
- 支持 DeepSeek 等 OpenAI-compatible 模型
- 多 Agent 任务编排
- 画像生成、计划生成、文档生成、问答、题库、批改、总结图生成
- 知识库召回增强

## 项目结构

```text
X-AgentStudy
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/         # 业务代码
│   ├── src/main/resources/    # 配置、Prompt、Flyway 迁移
│   └── src/test/java/         # 后端测试
├── frontend/                 # Vue 前端
│   ├── src/views/             # 页面
│   ├── src/components/        # 公共组件
│   ├── src/utils/             # 工具函数
│   └── src/api/               # 请求封装
└── docs/                     # 产品、架构、交付和部署文档
```

## 本地运行

### 1. 准备环境

- Java 17
- Maven 3.9+
- Node.js 22+
- MySQL
- Redis

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS x_agent_study CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置环境变量

复制 `.env.example` 后设置本机真实值。不要提交真实密钥。

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export DEEPSEEK_API_KEY=your_api_key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-chat
```

后端通过 Spring AI 调用 OpenAI-compatible 模型。默认示例使用 DeepSeek，也可以登录管理员账号后，在「管理后台 → 模型网关」配置其他兼容模型。

### 3. 启动后端

```bash
cd backend
DB_PASSWORD='your_password' \
DEEPSEEK_API_KEY='your_api_key' \
DEEPSEEK_BASE_URL='https://api.deepseek.com' \
DEEPSEEK_MODEL='deepseek-chat' \
mvn spring-boot:run
```

后端健康检查：

```text
http://localhost:8080/api/v1/health
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

前端地址：

```text
http://127.0.0.1:5173/
```

## 使用流程

1. 注册或登录账号。
2. 创建学习方向，例如 Redis、Spring Boot、机器学习等。
3. 生成学习画像，明确当前基础、学习目标和风险。
4. 生成学习计划，查看章节、知识点和任务。
5. 上传资料或直接使用计划内 AI 问答。
6. 生成讲义、题目、总结图，完成测验和复习。
7. 查看学习报告、薄弱点和周复盘。
8. 分享学习计划，或在问答社区中讨论问题。

## 验证

后端测试：

```bash
cd backend
DB_PASSWORD='your_password' mvn test
```

前端构建：

```bash
cd frontend
npm run build
```

当前已验证：

- 后端测试通过
- 前端构建通过
- 本地后端健康检查正常
- 本地前端页面可访问

## 交付文档

- 系统架构：`docs/architecture/system-architecture.md`
- API 设计：`docs/backend/api-design.md`
- 数据库设计：`docs/backend/database-design.md`
- 部署说明：`docs/ops/deployment-guide.md`
- A3 赛题查缺补漏：`docs/delivery/competition-gap-check.md`
- AI Coding 工具说明：`docs/delivery/ai-coding-tools.md`
- 项目实现报告：`docs/delivery/project-implementation-report.md`

## 迁移说明

代码仓库不包含本地数据库数据和上传文件。迁移到其他电脑时，如果需要保留现有学习记录、用户、题库、社区内容和上传资料，需要额外导出 MySQL 数据，并拷贝 `backend/uploads/`。
