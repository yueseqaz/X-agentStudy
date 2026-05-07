# Community QA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight question community where users can ask questions, answer in comments, and invite AI answers.

**Architecture:** Add a `community` backend module with JPA entities, repositories, service, controller, DTOs, prompts, and Flyway migration. Add one Vue view and a sidebar route. AI answers use the existing `ModelGateway` and a local fallback when no model is configured.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, JUnit/Mockito, Vue 3, TypeScript, Element Plus.

---

### Task 1: Backend Community Module

**Files:**
- Create: `backend/src/main/resources/db/migration/V21__community_qa.sql`
- Create: `backend/src/main/java/com/xagentstudy/community/*`
- Create: `backend/src/main/resources/prompts/community-answer-system.md`
- Create: `backend/src/main/resources/prompts/community-answer.md`
- Test: `backend/src/test/java/com/xagentstudy/community/CommunityServiceTest.java`

- [ ] Write failing tests for publishing a question, user answer, and `@ai` answer fallback.
- [ ] Run `cd backend && mvn -Dtest=CommunityServiceTest test` and confirm failure because classes do not exist.
- [ ] Implement entities, repositories, DTOs, service, controller, prompts, and migration.
- [ ] Run `cd backend && mvn -Dtest=CommunityServiceTest test` and confirm pass.

### Task 2: Frontend Community View

**Files:**
- Create: `frontend/src/views/CommunityView.vue`
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/style.css`

- [ ] Add `/community` route and sidebar item.
- [ ] Add question list, publish form, details panel, answer form, and AI invite button.
- [ ] Use existing `http` client and Element Plus components.
- [ ] Run `cd frontend && npm run build` and confirm pass.

### Task 3: Full Verification

- [ ] Run `cd backend && mvn test`.
- [ ] Run `cd frontend && npm run build`.
- [ ] Report any remaining limitation clearly.

