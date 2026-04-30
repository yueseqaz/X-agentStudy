请基于以下学习文档内容，提炼一份“知识总结图”结构化内容。

要求：
1. 输出合法 JSON。
2. 不要输出 Markdown。
3. 不要输出 HTML。
4. 不要照搬大段原文。
5. 内容要适合前端渲染为学习卡片图或长图。
6. 句子要短，重点要明确。
7. 核心要点控制在 3-6 条。
8. 关键词控制在 3-8 个。
9. 学习提醒或易错点控制在 0-3 条。
10. title 不超过 28 个汉字（或等效长度），避免图片标题换行过多。
11. summary 建议 1-2 句，总长度控制在 45-90 字。
12. highlights 每条建议 18-48 字，保证可读性。

输出格式：
{
  "title": "",
  "summary": "",
  "highlights": [],
  "keywords": [],
  "tips": [],
  "sourceDocumentId": "{{sourceDocumentId}}",
  "sourceDocumentName": "{{sourceDocumentName}}",
  "planId": "{{planId}}"
}

所属计划：{{planTitle}}
来源文档：{{sourceDocumentName}}

文档内容：
{{documentContent}}
