用户问题：{{question}}
用户水平：{{userLevel}}

当前学习计划上下文：
{{planContext}}

当前计划知识库召回内容：
{{retrievedContext}}

请生成严格 JSON：
{
  "answer": "string",
  "relatedPoints": ["string"],
  "suggestedQuestions": ["string"]
}

要求：
1. 回答必须强关联当前学习计划，优先围绕计划主题、目标、章节和知识点解释，不要泛泛回答。
2. 如果召回内容充足，优先基于召回内容回答。
3. 如果召回内容不足，进入计划冷启动模式，基于学习计划上下文和通用知识回答，并明确说明未引用上传资料。
4. 回答要适配用户水平，具体、可执行。
5. 不要把回答写成学习方法建议，必须回答用户问题本身。
6. answer 可以使用 Markdown，结构必须包含：直接回答 -> 原理解释 -> 小示例（或最小步骤）。
7. relatedPoints 返回 2-6 个，不得为空，必须来自或贴近当前计划。
8. suggestedQuestions 返回 2-4 个，且需和当前问题、当前计划连续相关。
