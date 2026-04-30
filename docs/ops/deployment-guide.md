# X-AgentStudy 部署文档（Docker + 他人电脑部署）

## 1. 部署目标

本文件提供两种可落地方式：

1. Docker 部署（推荐给演示/测试环境）
2. 在他人电脑本地部署（无需 Docker）

---

## 2. 环境变量说明

后端读取以下变量：

| 变量 | 示例 | 说明 |
|---|---|---|
| `DB_HOST` | `mysql` / `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `x_agent_study` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | `sakura` | 数据库密码 |
| `REDIS_HOST` | `redis` / `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `JWT_SECRET` | `please_change_me` | JWT 密钥（生产务必更换） |
| `JWT_EXPIRES_IN_SECONDS` | `604800` | JWT 有效期（秒） |
| `UPLOAD_ROOT` | `uploads` | 上传目录 |
| `DEEPSEEK_API_KEY` | `sk-...` | 大模型 key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 大模型地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | 大模型名称 |
| `EMBEDDING_API_KEY` | `sk-...` | embedding key（可选） |
| `EMBEDDING_BASE_URL` | `https://api.openai.com/v1` | embedding 地址（可选） |
| `EMBEDDING_MODEL` | `text-embedding-3-small` | embedding 模型（可选） |

---

## 3. Docker 部署（推荐）

## 3.1 前置条件

- Docker 24+
- Docker Compose v2+
- 可访问模型 API 的网络

## 3.2 在项目根目录新建文件

### `backend/Dockerfile`

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### `frontend/Dockerfile`

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
```

### `frontend/nginx.conf`

```nginx
server {
  listen 80;
  server_name _;

  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://backend:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

### `docker-compose.yml`（项目根目录）

```yaml
services:
  mysql:
    image: mysql:8.4
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: sakura
      MYSQL_DATABASE: x_agent_study
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  backend:
    build:
      context: ./backend
    restart: unless-stopped
    depends_on:
      - mysql
      - redis
    environment:
      SERVER_PORT: 8080
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: x_agent_study
      DB_USERNAME: root
      DB_PASSWORD: sakura
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: change_this_secret_in_real_env
      JWT_EXPIRES_IN_SECONDS: 604800
      UPLOAD_ROOT: /app/uploads
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      DEEPSEEK_BASE_URL: https://api.deepseek.com
      DEEPSEEK_MODEL: deepseek-chat
      EMBEDDING_API_KEY: ${EMBEDDING_API_KEY}
      EMBEDDING_BASE_URL: https://api.openai.com/v1
      EMBEDDING_MODEL: text-embedding-3-small
    volumes:
      - backend_uploads:/app/uploads
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "5173:80"

volumes:
  mysql_data:
  redis_data:
  backend_uploads:
```

## 3.3 启动

在项目根目录执行：

```bash
export DEEPSEEK_API_KEY=你的key
export EMBEDDING_API_KEY=你的embedding_key   # 可选
docker compose up -d --build
```

访问：

- 前端：`http://localhost:5173`
- 健康检查：`http://localhost:8080/api/v1/health`

## 3.4 验证

```bash
docker compose ps
docker compose logs -f backend
```

如果后端日志看到 Flyway migration 成功，数据库即初始化完成。

---

## 4. 在别人电脑部署（无 Docker）

## 4.1 需要安装

- Java 17
- Maven 3.9+
- Node.js 22+
- MySQL 8.x
- Redis 7.x

## 4.2 拿代码并配置

```bash
git clone <your-repo-url>
cd X-AgentStudy
cp .env.example .env.local
```

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS x_agent_study CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 4.3 启动后端

```bash
cd backend
DB_HOST=localhost \
DB_PORT=3306 \
DB_NAME=x_agent_study \
DB_USERNAME=root \
DB_PASSWORD=你的数据库密码 \
REDIS_HOST=localhost \
REDIS_PORT=6379 \
DEEPSEEK_API_KEY=你的key \
EMBEDDING_API_KEY=你的embedding_key \
mvn spring-boot:run
```

## 4.4 启动前端

新开终端：

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

访问：

- 同机访问：`http://127.0.0.1:5173`
- 局域网访问：`http://你的电脑IP:5173`

---

## 5. 首次登录建议

- 可以直接注册新账号。
- 系统内置演示账号（若未被修改）：
  - `demo@xagentstudy.local`
  - `demo123456`

---

## 6. 常见问题排查

### 6.1 后端启动报数据库连接失败

检查 `DB_HOST/DB_PORT/DB_USERNAME/DB_PASSWORD`，并确认 MySQL 已启动。

### 6.2 进入页面出现 401

前端本地 token 过期，退出后重新登录。

### 6.3 AI 功能无输出

检查：
- `DEEPSEEK_API_KEY` 是否有效
- 管理后台模型配置是否启用且连通

### 6.4 检索效果不稳定

当前为“远端 embedding 优先，失败回退本地 embedding”机制。  
若要稳定高质量检索，建议配置可用的远端 embedding key。

---

## 7. 上线前最小安全清单

1. 修改 `JWT_SECRET`
2. 限制数据库与 Redis 外网暴露
3. 使用 HTTPS（反向代理层）
4. 分离演示账号与真实业务账号
5. 对上传目录做好备份和访问控制

