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
