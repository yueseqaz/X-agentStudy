用户问题：{{question}}
用户水平：{{userLevel}}

当前计划知识库召回内容：
{{retrievedContext}}

请生成严格 JSON：
{
  "answer": "string",
  "relatedPoints": ["string"],
  "suggestedQuestions": ["string"]
}

要求：
1. 如果召回内容充足，优先基于召回内容回答。
2. 如果召回内容不足，进入无知识库模式，基于通用知识回答并明确说明未引用计划资料。
3. 回答要适配用户水平，具体、可执行。
4. 不要把回答写成学习方法建议，必须回答用户问题本身。
5. answer 结构必须包含：直接回答 -> 原理解释 -> 小示例（或最小步骤）。
6. relatedPoints 返回 2-6 个，不得为空。
7. suggestedQuestions 返回 2-4 个，且需和当前问题连续相关。
