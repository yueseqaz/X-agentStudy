# 数据库设计草案

## 命名约定

- 表名使用小写下划线。
- 主键统一为 `id`，建议使用雪花 ID、UUID 或数据库自增。
- 通用字段：`created_at`、`updated_at`、`deleted_at`。
- 建议采用逻辑删除，并结合数据保留、账号注销和审计要求设计清理策略。

## 表清单

### users

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 用户 ID |
| nickname | varchar(64) | 昵称 |
| account | varchar(128) | 账号 |
| password_hash | varchar(255) | 密码哈希 |
| created_at | datetime | 创建时间 |

### learning_directions

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 方向 ID |
| user_id | bigint | 用户 ID |
| name | varchar(128) | 方向名称 |
| category | varchar(64) | 技能分类 |
| description | text | 补充说明 |
| last_active_at | datetime | 最近活跃时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| deleted_at | datetime | 删除时间 |

### learning_profiles

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 画像 ID |
| direction_id | bigint | 方向 ID |
| goal | text | 学习目标 |
| current_level | varchar(64) | 当前水平 |
| time_budget | varchar(128) | 时间投入 |
| preference | text | 学习偏好 |
| risks | json | 风险点 |
| strategy | text | 推荐策略 |
| raw_conversation | json | 画像对话记录 |
| created_at | datetime | 生成时间 |

### learning_plans

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 计划 ID |
| direction_id | bigint | 方向 ID |
| profile_id | bigint | 画像 ID |
| title | varchar(128) | 计划名称 |
| status | varchar(32) | 计划状态 |
| goal | text | 总目标 |
| stages | json | 阶段结构 |
| current_stage_index | int | 当前阶段 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### documents

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 文档 ID |
| plan_id | bigint | 计划 ID |
| name | varchar(255) | 文档名称 |
| type | varchar(32) | 文件类型 |
| storage_key | varchar(512) | 文件存储路径 |
| parse_status | varchar(32) | 解析状态 |
| summary | text | 文档摘要 |
| parse_error | text | 失败原因 |
| uploaded_at | datetime | 上传时间 |
| updated_at | datetime | 更新时间 |

### knowledge_chunks

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 切片 ID |
| plan_id | bigint | 计划 ID |
| document_id | bigint | 文档 ID |
| chunk_index | int | 切片序号 |
| content | text | 切片内容 |
| source_location | varchar(255) | 页码/章节 |
| vector_id | varchar(128) | 向量库 ID |
| created_at | datetime | 创建时间 |

### qa_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 问答 ID |
| plan_id | bigint | 计划 ID |
| user_id | bigint | 用户 ID |
| question | text | 用户问题 |
| answer | text | Agent 回答 |
| citations | json | 引用来源 |
| related_points | json | 相关知识点 |
| created_at | datetime | 时间 |

### questions

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 题目 ID |
| plan_id | bigint | 计划 ID |
| source_scope | varchar(64) | 来源范围 |
| type | varchar(32) | 题型 |
| difficulty | varchar(32) | 难度 |
| stem | text | 题干 |
| options | json | 选项 |
| standard_answer | json | 标准答案 |
| explanation | text | 解析 |
| knowledge_points | json | 知识点 |
| created_at | datetime | 生成时间 |

### answer_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 作答 ID |
| question_id | bigint | 题目 ID |
| user_id | bigint | 用户 ID |
| user_answer | json | 用户答案 |
| is_correct | boolean | 是否正确 |
| answered_at | datetime | 作答时间 |

### review_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 复习 ID |
| plan_id | bigint | 计划 ID |
| knowledge_point | varchar(255) | 知识点 |
| priority | int | 优先级 |
| reason | text | 推荐原因 |
| due_at | datetime | 建议复习时间 |
| completed_at | datetime | 完成时间 |
| created_at | datetime | 创建时间 |

### agent_tasks

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 任务 ID |
| plan_id | bigint | 计划 ID，可为空 |
| task_type | varchar(64) | 任务类型 |
| status | varchar(32) | 任务状态 |
| input_payload | json | 输入 |
| output_payload | json | 输出 |
| error_message | text | 错误 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### subscriptions

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 订阅 ID |
| user_id | bigint | 用户 ID |
| plan_code | varchar(64) | 套餐编码 |
| status | varchar(32) | 订阅状态 |
| started_at | datetime | 开始时间 |
| expired_at | datetime | 到期时间 |
| created_at | datetime | 创建时间 |

### usage_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 用量 ID |
| user_id | bigint | 用户 ID |
| plan_id | bigint | 学习计划 ID，可为空 |
| usage_type | varchar(64) | 用量类型 |
| amount | int | 消耗数量 |
| related_task_id | bigint | 关联任务 |
| created_at | datetime | 创建时间 |

### notifications

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 通知 ID |
| user_id | bigint | 用户 ID |
| type | varchar(64) | 通知类型 |
| title | varchar(128) | 标题 |
| content | text | 内容 |
| read_at | datetime | 已读时间 |
| created_at | datetime | 创建时间 |

### admin_audit_logs

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 日志 ID |
| actor_user_id | bigint | 操作人 |
| action | varchar(128) | 操作 |
| target_type | varchar(64) | 对象类型 |
| target_id | varchar(128) | 对象 ID |
| detail | json | 明细 |
| created_at | datetime | 创建时间 |

## 索引建议

- `learning_directions(user_id, deleted_at)`
- `learning_plans(direction_id, status)`
- `documents(plan_id, parse_status)`
- `knowledge_chunks(plan_id, document_id)`
- `qa_records(plan_id, created_at)`
- `questions(plan_id, difficulty, type)`
- `answer_records(question_id, user_id)`
- `review_records(plan_id, due_at, completed_at)`
- `agent_tasks(plan_id, task_type, status)`
- `subscriptions(user_id, status)`
- `usage_records(user_id, usage_type, created_at)`
- `notifications(user_id, read_at, created_at)`
