学习计划：{{planTitle}}
运行模式：{{mode}}
难度：{{difficulty}}
题型：{{questionType}}
题量：{{count}}

可用上下文：
{{context}}

请生成严格 JSON：
{
  "questions": [
    {
      "type": "SINGLE_CHOICE|FILL_BLANK|SHORT_ANSWER|CODE|CASE_ANALYSIS|INTERVIEW",
      "difficulty": "EASY|MEDIUM|HARD",
      "stem": "string",
      "options": [
        {"key":"A","text":"string"},
        {"key":"B","text":"string"},
        {"key":"C","text":"string"},
        {"key":"D","text":"string"}
      ],
      "standardAnswer": ["A"],
      "explanation": "string",
      "knowledgePoints": ["string"]
    }
  ]
}

要求：
1. 必须严格生成 {{count}} 道题，不能少于题量。
2. 题目必须围绕学习计划；知识库增强模式必须只依据“可用上下文”中的文档内容和知识点出题。
3. 如果上下文是 Redis，就只能围绕 Redis 文档内容出题；如果上下文是 Java，就只能围绕 Java 文档内容出题，以此类推。
4. knowledgePoints 必须包含当前知识点或上下文中的具体概念。
5. 无知识库模式可以基于通用知识生成但不得伪造资料引用。
6. 只输出 JSON。
7. 选择题 options 必须 4 个且互斥，避免多个同义正确项。
8. FILL_BLANK 的 standardAnswer 必须给可判分文本（可含同义词数组）。
9. 主观题/代码题/案例题/面试题必须在 explanation 中给出评分要点。
10. stem 必须清晰完整，避免“以下哪项正确”但选项无可判定依据。
