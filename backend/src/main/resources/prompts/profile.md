学习方向：{{directionName}}
分类：{{category}}
补充说明：{{description}}

用户回答：
{{answers}}

请生成严格 JSON：
{
  "goal": "string",
  "currentLevel": "BEGINNER|FOUNDATION|INTERMEDIATE|ADVANCED",
  "timeBudget": "string",
  "preference": "string",
  "risks": ["string"],
  "strategy": "string"
}

要求：
1. 目标要贴合学习方向和用户回答，不要泛泛写“提升能力”。
2. 当前水平必须基于回答判断。
3. strategy 要能直接作为规划 Agent 的输入。
4. 如果回答信息有限，允许保守推断，但字段不能留空。
5. risks 至少给出 2 条，且必须和用户场景相关。
6. timeBudget 尽量量化到“每天/每周 + 时长”。
7. 必须覆盖不少于 6 个画像维度：学习目标、知识基础、学习节奏、认知风格、易错点、资源偏好。
8. preference 需要同时体现认知风格与资源偏好，例如“图解优先、项目驱动、先看示例再练习”。
9. risks 需要体现易错点、知识短板或执行风险，不能只写泛泛的学习态度问题。
