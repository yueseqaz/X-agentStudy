# Task Plan: 赛题查缺补漏

## Goal
根据 A3 赛题材料对照当前项目，整理缺口并补齐可以直接完成的内容。

## Current Phase
Phase 5

## Phases

### Phase 1: 赛题与项目现状梳理
- [x] 提取赛题要求
- [x] 查看项目结构与已有功能
- [x] 记录发现到 findings.md
- **Status:** complete

### Phase 2: 缺口清单
- [x] 对照赛题要求与项目现状
- [x] 区分可直接补齐、需要用户确认、后续建议
- **Status:** complete

### Phase 3: 补齐与调整
- [x] 修改必要文件
- [x] 保持改动范围聚焦
- **Status:** complete

### Phase 4: 验证
- [x] 执行可用的验证命令
- [x] 修复验证发现的问题
- **Status:** complete

### Phase 5: 交付
- [x] 汇总实际改动
- [x] 汇总验证结果
- [x] 标明剩余待确认项
- **Status:** complete

## Key Questions
1. A3 赛题要求的核心交付物有哪些？
2. 当前项目已经覆盖哪些要求？
3. 哪些缺口可以直接补齐？
4. 哪些缺口需要产品方向或数据来源确认？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 先做赛题-项目对照，再动手修改 | 避免偏离赛题要求 |
| 优先补“评审可见性”缺口 | 项目已有较多能力，赛题表达和提交材料更容易影响初赛判断 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| zsh: command not found: python | 1 | 改用 python3 运行 session-catchup.py |
| ProfilePromptContractTest 首次失败 | 1 | 补强画像提示词 6 维规则后通过 |
