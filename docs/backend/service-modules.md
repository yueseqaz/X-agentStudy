# 后端服务模块

## 包结构建议

```text
com.xagentstudy
  common
    config
    exception
    response
    security
  user
  direction
  plan
  profile
  knowledge
  qa
  quiz
  review
  report
  notification
  subscription
  admin
  organization
  agent
    orchestration
    prompt
    model
    task
  rag
    document
    chunk
    embedding
    retrieval
  async
```

## 模块职责

| 模块 | 职责 |
| --- | --- |
| user | 用户信息、登录态、JWT 鉴权 |
| direction | 学习方向 CRUD |
| profile | 画像对话、画像生成、画像确认 |
| plan | 学习计划生成、阶段任务、计划状态 |
| knowledge | 文档上传、文档元数据、解析状态、摘要 |
| qa | 计划内问答、引用来源、追问 |
| quiz | 测验生成、题库、作答、错题 |
| review | 复习建议、薄弱点、完成状态 |
| report | 日报、周报、学习反馈 |
| notification | 站内提醒、复习提醒、任务提醒 |
| subscription | 套餐、额度、用量统计、支付接口预留 |
| admin | 管理后台、运营指标、异常任务、模型调用监控 |
| organization | 团队/企业组织、成员、角色 |
| agent | Agent 编排、Prompt 模板、结构化输出 |
| rag | 文档解析、切片、向量化、召回、来源映射 |
| async | 异步任务状态、重试、任务调度 |

## 统一响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "req_xxx"
}
```

## 统一异常

| 错误码 | 场景 |
| --- | --- |
| `VALIDATION_ERROR` | 参数校验失败 |
| `UNAUTHORIZED` | 未登录或登录失效 |
| `FORBIDDEN` | 无权访问该计划 |
| `RESOURCE_NOT_FOUND` | 数据不存在 |
| `AGENT_TASK_FAILED` | Agent 任务失败 |
| `DOCUMENT_PARSE_FAILED` | 文档解析失败 |
| `RATE_LIMITED` | 调用过于频繁 |

## 权限校验

完整版本必须启用用户身份和权限边界：

- 查询方向：校验 `direction.user_id == current_user_id`。
- 查询计划：通过方向反查用户归属。
- 查询计划级资料、题库、问答、复习：校验 `plan_id` 归属。
