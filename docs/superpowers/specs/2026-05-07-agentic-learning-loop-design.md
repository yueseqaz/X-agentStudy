# Agentic Learning Loop Design

## Goal

Make X-AgentStudy clearly behave as a learning execution system rather than a chat-only assistant.

## Scope

- Add a task-centered view that shows the current learning objective, task status, and next action.
- Add an Agent work log so teachers can see what the system has done and what each result changed.
- Strengthen the multi-agent display with real evidence from plan, tasks, documents, quizzes, reviews, and agent task records.

## Recommended Approach

Use the existing `WorkflowView` as the main proof page. It already gathers plan, tasks, documents, reports, reviews, resources, dynamic profile, and resource quality data. Enhancing this page avoids adding a separate dashboard that repeats the same information.

## User Experience

The page should show:

- A "今日学习任务中心" section with active task, task state, reason, and next action.
- A "Agent 工作记录" section with recent agent tasks for the current plan.
- Existing multi-agent cards should remain, but the page should explain each agent through visible evidence instead of labels only.

## Backend

- Add an endpoint to list recent agent tasks by plan.
- Return newest tasks first.
- Keep the existing single-task lookup endpoint.

## Validation

- Backend test proves plan-level agent task records are returned newest first.
- Frontend build proves the updated view compiles.
- Existing backend tests should still pass.
