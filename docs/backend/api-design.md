# API 设计草案

## 基础约定

- Base URL: `/api/v1`
- 请求和响应均使用 JSON，文件上传使用 `multipart/form-data`。
- 计划级接口必须包含 `planId`，服务端校验访问权限。
- 长任务优先返回 `taskId`，前端通过任务接口查询状态。

## 账号与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/auth/register` | 注册 |
| `POST` | `/auth/login` | 登录 |
| `POST` | `/auth/logout` | 登出 |
| `GET` | `/me` | 当前用户 |
| `PUT` | `/me` | 更新个人资料 |

## 学习方向

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/directions` | 获取方向列表 |
| `POST` | `/directions` | 创建学习方向 |
| `GET` | `/directions/{directionId}` | 获取方向详情 |
| `PUT` | `/directions/{directionId}` | 更新方向 |
| `DELETE` | `/directions/{directionId}` | 删除方向 |

### 创建方向请求

```json
{
  "name": "Java 后端",
  "category": "编程开发",
  "description": "希望 4 个月内具备转岗能力"
}
```

## 画像 Agent

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/directions/{directionId}/profile/conversation` | 提交画像对话回答 |
| `POST` | `/directions/{directionId}/profile/generate` | 生成学习画像 |
| `GET` | `/profiles/{profileId}` | 获取画像 |
| `POST` | `/profiles/{profileId}/confirm` | 确认画像 |

## 学习计划

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/directions/{directionId}/plans/generate` | 基于画像生成计划 |
| `GET` | `/plans/{planId}` | 获取计划详情 |
| `PUT` | `/plans/{planId}` | 微调计划 |
| `POST` | `/plans/{planId}/accept` | 接受计划 |
| `POST` | `/plans/{planId}/tasks/{taskId}/complete` | 标记任务完成 |

## 知识库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/plans/{planId}/documents` | 文档列表 |
| `POST` | `/plans/{planId}/documents` | 上传文档 |
| `GET` | `/plans/{planId}/documents/{documentId}` | 文档详情 |
| `DELETE` | `/plans/{planId}/documents/{documentId}` | 删除文档 |
| `POST` | `/plans/{planId}/documents/{documentId}/retry-parse` | 重试解析 |

### 上传响应

```json
{
  "documentId": "1001",
  "taskId": "9001",
  "parseStatus": "PARSING"
}
```

## 问答

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/plans/{planId}/qa` | 发起计划内问答 |
| `GET` | `/plans/{planId}/qa-records` | 获取问答历史 |
| `GET` | `/plans/{planId}/qa-records/{qaId}` | 获取问答详情 |

### 问答请求

```json
{
  "question": "Spring MVC 的 Controller 和 Service 应该怎么分工？",
  "style": "simple"
}
```

规则：当当前计划无资料或无召回结果时，接口仍返回回答，`citations` 为空，并在回答内容中标识“未引用计划资料”。

## 测验与题库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/plans/{planId}/quizzes/generate` | 生成题目 |
| `GET` | `/plans/{planId}/questions` | 查询题库 |
| `POST` | `/plans/{planId}/questions/{questionId}/answers` | 提交答案 |
| `GET` | `/plans/{planId}/wrong-questions` | 查询错题 |

### 生成题目请求

```json
{
  "difficulty": "MEDIUM",
  "count": 5,
  "types": ["SINGLE_CHOICE", "SHORT_ANSWER"],
  "scope": {
    "type": "DOCUMENT",
    "documentIds": ["1001"]
  }
}
```

规则：当当前计划无资料或无召回结果时，接口仍生成通用题目，题目 `sourceScope` 标记为 `COLD_START`；有资料命中时使用 `CHUNK:{chunkId}`。

## 复习与报告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/plans/{planId}/reviews/today` | 今日复习 |
| `POST` | `/plans/{planId}/reviews/{reviewId}/complete` | 标记复习完成 |
| `GET` | `/plans/{planId}/reports/latest` | 最新学习报告 |
| `POST` | `/plans/{planId}/reports/generate` | 生成学习报告 |

## 异步任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/tasks/{taskId}` | 查询任务状态 |

## 订阅与额度

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/billing/plans` | 套餐列表 |
| `GET` | `/billing/subscription` | 当前订阅 |
| `GET` | `/usage/summary` | 当前用量概览 |
| `POST` | `/billing/checkout` | 创建支付会话，支付实现可预留 |

## 通知

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/notifications` | 通知列表 |
| `POST` | `/notifications/{notificationId}/read` | 标记已读 |

## 管理后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/admin/metrics/overview` | 运营指标概览 |
| `GET` | `/admin/agent-tasks` | Agent 任务监控 |
| `GET` | `/admin/users` | 用户列表 |
| `GET` | `/admin/usage` | 用量统计 |

### 任务响应

```json
{
  "taskId": "9001",
  "taskType": "DOCUMENT_PARSE",
  "status": "RUNNING",
  "progress": 60,
  "message": "正在生成文档摘要"
}
```
