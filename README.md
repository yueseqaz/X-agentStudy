# X-AgentStudy

多 Agent 协作技能学习 SaaS 平台。

## 当前状态

已完成主要学习平台功能：

- `backend/`：Spring Boot、MySQL、Redis、Flyway、认证授权、学习计划、知识库、题库、复习、报告、资源库、管理后台接口。
- `frontend/`：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Element Plus、工作台、学习方向、计划详情、节点式工作流、资源库、管理后台、新手引导。
- `docs/`：产品、架构、后端、前端、AI、部署和交付文档。

## 本地环境

- Java 17
- Maven 3.9+
- Node.js 22+
- MySQL
- Redis

## 数据库

```sql
CREATE DATABASE IF NOT EXISTS x_agent_study CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway 会在后端启动时自动创建核心表。

## 环境变量

复制 `.env.example` 后在本机 shell 或 IDE 运行配置中设置真实值。不要提交真实密钥。

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export DEEPSEEK_API_KEY=your_api_key
```

文档生成、画像、计划、问答、题库等 AI 功能需要可用模型 Key。也可以登录管理员账号后，在「管理后台 → 模型网关」配置模型。

如果 Maven 中央仓库下载慢，可以在本机 `~/.m2/settings.xml` 配置镜像，不建议把镜像写死到 `pom.xml`：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

## 启动后端

```bash
cd backend
DB_PASSWORD='your_password' mvn spring-boot:run
```

启用 DeepSeek：

```bash
cd backend
DB_PASSWORD='your_password' DEEPSEEK_API_KEY='your_api_key' mvn spring-boot:run
```

如果不提供可用模型 Key，AI 生成类能力会受限；文档生成不会再静默返回本地模板，而是提示先配置模型。

后端地址：

- `http://localhost:8080/api/v1/health`
- `http://localhost:8080/api/v1/directions`
- `POST http://localhost:8080/api/v1/directions/{directionId}/profile/generate`
- `POST http://localhost:8080/api/v1/directions/{directionId}/plans/generate`

## 启动前端

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

前端地址：

- `http://127.0.0.1:5173/`

## 验证

```bash
cd backend
DB_PASSWORD='your_password' mvn test
```

```bash
cd frontend
npm run build
```

## 部署资料

- 部署说明：`docs/ops/deployment-guide.md`
- 数据库设计：`docs/backend/database-design.md`
- API 设计：`docs/backend/api-design.md`
- 系统架构：`docs/architecture/system-architecture.md`

迁移到其他电脑时，代码不包含数据库数据和上传文件。如需保留现有数据，需要额外导出 MySQL，并拷贝 `backend/uploads/`。
