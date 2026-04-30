# Prompt Templates

后端 Agent 优先从本目录读取 prompt 模板，避免提示词散落在业务代码里。

- `plan-course.md`：规划 Agent，生成可解析课程计划 JSON。
- `document-lecture.md`：在线讲义 Agent，围绕单个知识点生成 Markdown 正文。
- `profile.md` / `profile-system.md`：画像 Agent。
- `tutor.md` / `tutor-system.md`：答疑 Agent。
- `quiz.md` / `quiz-system.md`：测验 Agent。
- `grader.md` / `grader-system.md`：主观题批改 Agent。
- `document-system.md`：在线讲义 Agent 的系统提示词。

模板变量使用 `{{name}}` 格式，由 `AgentGenerationService` 渲染。
