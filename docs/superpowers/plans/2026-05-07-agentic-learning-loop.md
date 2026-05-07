# Agentic Learning Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make X-AgentStudy visibly act like a learning execution system through a task center, agent work log, and closed-loop next action.

**Architecture:** Reuse the existing plan workflow page as the proof surface. Add one backend list endpoint for plan-scoped agent tasks, then render those records beside task progress and multi-agent evidence.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Vue 3, TypeScript, Element Plus.

---

### Task 1: Plan-Scoped Agent Task Records

**Files:**
- Modify: `backend/src/main/java/com/xagentstudy/agent/task/AgentTaskRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/agent/task/AgentTaskService.java`
- Modify: `backend/src/main/java/com/xagentstudy/agent/task/AgentTaskController.java`
- Test: `backend/src/test/java/com/xagentstudy/agent/task/AgentTaskServiceTest.java`

- [ ] Add a service test requiring `listByPlan(Long planId)` to return the newest records first.
- [ ] Add `findTop20ByPlanIdOrderByUpdatedAtDesc(Long planId)` to the repository.
- [ ] Add `listByPlan` to the service.
- [ ] Add `GET /api/v1/tasks/plans/{planId}` to the controller.
- [ ] Run `cd backend && DB_PASSWORD='test' mvn -Dtest=AgentTaskServiceTest test`.

### Task 2: Workflow Task Center

**Files:**
- Modify: `frontend/src/views/WorkflowView.vue`

- [ ] Add current task and next-action computed values from existing plan tasks, documents, quizzes, and reviews.
- [ ] Render "今日学习任务中心" near the top of the workflow page.
- [ ] Keep actions routed to the existing plan tabs.
- [ ] Make the section readable on mobile and desktop.

### Task 3: Agent Work Log

**Files:**
- Modify: `frontend/src/views/WorkflowView.vue`

- [ ] Fetch `/tasks/plans/{planId}` with the existing workflow data.
- [ ] Render "Agent 工作记录" using task type, status, update time, and output/error summary.
- [ ] Show a meaningful empty state when no records exist.
- [ ] Run `cd frontend && npm run build`.

### Task 4: Full Verification

**Files:**
- No production file changes.

- [ ] Run `cd backend && DB_PASSWORD='test' mvn test`.
- [ ] Run `cd frontend && npm run build`.
- [ ] Confirm the workflow page communicates planning, execution, record, judgment, and next action.
