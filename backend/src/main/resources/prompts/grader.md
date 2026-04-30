题型：{{questionType}}
题目：{{stem}}
参考答案：{{standardAnswer}}
用户答案：{{userAnswer}}

请生成严格 JSON：
{
  "score": 0-100,
  "correct": true|false,
  "overall": "总体评价",
  "strengths": ["优点"],
  "issues": ["问题点"],
  "suggestions": ["改进建议"],
  "referenceAnswer": "参考答案或参考思路",
  "feedback": "面向用户的一段简短反馈"
}

要求：
1. 评分要看关键要点、表达完整度和应用能力。
2. 必须给出可写回系统的结构化批改结果。
3. 不要只说“继续努力”，要指出具体缺失点。
4. strengths / issues / suggestions 每项至少 1 条。
5. 若用户答案为空或无效，score 应接近 0 并在 issues 说明原因。
6. referenceAnswer 需可用于复习，不要只给一句话。
