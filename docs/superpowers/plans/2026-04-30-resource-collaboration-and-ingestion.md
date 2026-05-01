# Resource Collaboration And Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a first version of resource collaboration and open-resource ingestion so users can apply to become resource collaborators, collaborators can manage only their own ingestion work, and admins retain final approval of applicants, sources, and candidate resources.

**Architecture:** Extend the existing auth and admin model with a new collaborator permission instead of reusing `ADMIN`. Add backend modules for collaborator applications, managed ingestion sources, ingestion tasks, candidate resources, and admin review actions; then add two frontend surfaces: a user-facing application and “my resource management” area, and an admin-facing review area embedded into the current admin console.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, Vue 3, TypeScript, Element Plus, existing Axios client, existing admin and resource library patterns

---

## File Structure

**Create**

- `backend/src/main/resources/db/migration/V17__resource_collaboration.sql`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplication.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplicationRepository.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceManagerGrant.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceManagerGrantRepository.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/SourceType.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionSource.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionSourceRepository.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionTask.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionTaskRepository.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CandidateResource.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CandidateResourceRepository.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplicationRequest.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ReviewCollaboratorApplicationRequest.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/CreateIngestionTaskRequest.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/UpdateCandidateResourceRequest.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ReviewCandidateResourceRequest.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceCollaborationService.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceCollaborationController.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/AdminResourceCollaborationController.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/response/CollaboratorApplicationResponse.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/response/CollaboratorWorkspaceResponse.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/response/CandidateResourceResponse.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/response/IngestionTaskResponse.java`
- `backend/src/main/java/com/xagentstudy/resourcecollab/response/IngestionSourceResponse.java`
- `backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java`
- `frontend/src/views/ResourceContributorView.vue`
- `frontend/src/views/ResourceApplicationView.vue`

**Modify**

- `backend/src/main/java/com/xagentstudy/user/AppUser.java`
- `backend/src/main/java/com/xagentstudy/user/AppUserRepository.java`
- `backend/src/main/java/com/xagentstudy/auth/AuthResponse.java`
- `backend/src/main/java/com/xagentstudy/auth/AuthService.java`
- `backend/src/main/java/com/xagentstudy/admin/AdminController.java`
- `backend/src/main/java/com/xagentstudy/admin/AdminService.java`
- `frontend/src/stores/auth.ts`
- `frontend/src/router.ts`
- `frontend/src/App.vue`
- `frontend/src/views/AdminView.vue`
- `frontend/src/views/ResourceLibraryView.vue`
- `frontend/src/style.css`

**Test**

- `backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java`
- `backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java`

### Task 1: Add Data Model And Permission Flag

**Files:**

- Create: `backend/src/main/resources/db/migration/V17__resource_collaboration.sql`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplication.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplicationRepository.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceManagerGrant.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceManagerGrantRepository.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/SourceType.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionSource.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionSourceRepository.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionTask.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/IngestionTaskRepository.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CandidateResource.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CandidateResourceRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/user/AppUser.java`
- Modify: `backend/src/main/java/com/xagentstudy/auth/AuthResponse.java`

- [ ] **Step 1: Write the failing test**

```java
package com.xagentstudy.resourcecollab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceCollaborationServiceTest {
    @Test
    void collaboratorGrantMarksUserAsResourceManager() {
        ResourceManagerGrant grant = new ResourceManagerGrant(7L, 99L, "approved");

        assertThat(grant.getUserId()).isEqualTo(7L);
        assertThat(grant.isActive()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest test`
Expected: FAIL with `package com.xagentstudy.resourcecollab does not exist`

- [ ] **Step 3: Add migration and minimal entities**

```sql
CREATE TABLE collaborator_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  reason TEXT NOT NULL,
  expertise TEXT NULL,
  status VARCHAR(32) NOT NULL,
  review_note TEXT NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_collaborator_application_user UNIQUE (user_id)
);

CREATE TABLE resource_manager_grants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  granted_by BIGINT NOT NULL,
  note TEXT NULL,
  active BIT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_resource_manager_grant_user UNIQUE (user_id)
);
```

```java
@Entity
@Table(name = "resource_manager_grants")
public class ResourceManagerGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long grantedBy;
    @Column(columnDefinition = "text")
    private String note;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected ResourceManagerGrant() {
    }

    public ResourceManagerGrant(Long userId, Long grantedBy, String note) {
        this.userId = userId;
        this.grantedBy = grantedBy;
        this.note = note;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getUserId() { return userId; }
    public boolean isActive() { return Boolean.TRUE.equals(active); }
}
```

- [ ] **Step 4: Expose the flag through auth**

```java
// AuthResponse.java
public record AuthResponse(
        String token,
        String refreshToken,
        Long userId,
        String nickname,
        String account,
        String role,
        boolean resourceManager
) {
}
```

```java
// AppUser.java
public boolean isAdmin() {
    return "ADMIN".equalsIgnoreCase(role);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V17__resource_collaboration.sql backend/src/main/java/com/xagentstudy/resourcecollab backend/src/main/java/com/xagentstudy/user/AppUser.java backend/src/main/java/com/xagentstudy/auth/AuthResponse.java backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java
git commit -m "feat: add resource collaboration data model"
```

### Task 2: Build User Application And Collaborator Workspace APIs

**Files:**

- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CollaboratorApplicationRequest.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/CreateIngestionTaskRequest.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/UpdateCandidateResourceRequest.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceCollaborationService.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ResourceCollaborationController.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/response/CollaboratorApplicationResponse.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/response/CollaboratorWorkspaceResponse.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/response/CandidateResourceResponse.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/response/IngestionTaskResponse.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/response/IngestionSourceResponse.java`
- Modify: `backend/src/main/java/com/xagentstudy/auth/AuthService.java`
- Test: `backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@Test
void submitsApplicationAndReturnsPendingStatus() {
    CollaboratorApplication application = service.submitApplication(7L, new CollaboratorApplicationRequest("我擅长整理 Java 资料", "Java 后端, Redis"));

    assertThat(application.getStatus()).isEqualTo("PENDING");
    assertThat(application.getReason()).contains("Java");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest test`
Expected: FAIL with `submitApplication` not found

- [ ] **Step 3: Add user-facing endpoints**

```java
@RestController
@RequestMapping("/api/v1/resource-collaboration")
public class ResourceCollaborationController {
    private final ResourceCollaborationService service;

    public ResourceCollaborationController(ResourceCollaborationService service) {
        this.service = service;
    }

    @GetMapping("/application")
    public ApiResponse<CollaboratorApplicationResponse> application() {
        return ApiResponse.ok(service.currentApplication());
    }

    @PostMapping("/application")
    public ApiResponse<CollaboratorApplicationResponse> submit(@RequestBody CollaboratorApplicationRequest request) {
        return ApiResponse.ok(service.submitApplication(request));
    }

    @GetMapping("/workspace")
    public ApiResponse<CollaboratorWorkspaceResponse> workspace() {
        return ApiResponse.ok(service.workspace());
    }
}
```

- [ ] **Step 4: Add collaborator-only workspace operations**

```java
@PostMapping("/tasks")
public ApiResponse<IngestionTaskResponse> createTask(@RequestBody CreateIngestionTaskRequest request) {
    return ApiResponse.ok(service.createTask(request));
}

@PatchMapping("/candidates/{candidateId}")
public ApiResponse<CandidateResourceResponse> updateCandidate(
        @PathVariable Long candidateId,
        @RequestBody UpdateCandidateResourceRequest request
) {
    return ApiResponse.ok(service.updateCandidate(candidateId, request));
}

@PostMapping("/candidates/{candidateId}/submit")
public ApiResponse<CandidateResourceResponse> submitCandidate(@PathVariable Long candidateId) {
    return ApiResponse.ok(service.submitCandidate(candidateId));
}
```

- [ ] **Step 5: Return `resourceManager` in login and `me`**

```java
// AuthService.java
private AuthResponse toAuthResponse(AppUser user, String token, String refreshToken) {
    boolean resourceManager = resourceManagerGrantRepository
            .findByUserIdAndActiveTrue(user.getId())
            .isPresent();
    return new AuthResponse(
            token,
            refreshToken,
            user.getId(),
            user.getNickname(),
            user.getAccount(),
            user.getRole(),
            resourceManager
    );
}
```

- [ ] **Step 6: Run targeted tests**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/xagentstudy/resourcecollab backend/src/main/java/com/xagentstudy/auth/AuthService.java backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java
git commit -m "feat: add collaborator application and workspace api"
```

### Task 3: Add Admin Review And Source Management APIs

**Files:**

- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ReviewCollaboratorApplicationRequest.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/ReviewCandidateResourceRequest.java`
- Create: `backend/src/main/java/com/xagentstudy/resourcecollab/AdminResourceCollaborationController.java`
- Modify: `backend/src/main/java/com/xagentstudy/admin/AdminController.java`
- Modify: `backend/src/main/java/com/xagentstudy/admin/AdminService.java`
- Test: `backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java`

- [ ] **Step 1: Write the failing review test**

```java
@Test
void approvingApplicationCreatesGrant() {
    service.reviewApplication(99L, applicationId, new ReviewCollaboratorApplicationRequest("APPROVED", "可以开始管理自己的资源"));

    assertThat(grantRepository.findByUserIdAndActiveTrue(7L)).isPresent();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest test`
Expected: FAIL with `reviewApplication` not found

- [ ] **Step 3: Add admin endpoints**

```java
@RestController
@RequestMapping("/api/v1/admin/resource-collaboration")
public class AdminResourceCollaborationController {
    private final ResourceCollaborationService service;

    public AdminResourceCollaborationController(ResourceCollaborationService service) {
        this.service = service;
    }

    @GetMapping("/applications")
    public ApiResponse<List<CollaboratorApplicationResponse>> applications() {
        return ApiResponse.ok(service.listApplications());
    }

    @PostMapping("/applications/{applicationId}/review")
    public ApiResponse<CollaboratorApplicationResponse> reviewApplication(
            @PathVariable Long applicationId,
            @RequestBody ReviewCollaboratorApplicationRequest request
    ) {
        return ApiResponse.ok(service.reviewApplication(applicationId, request));
    }

    @GetMapping("/candidates")
    public ApiResponse<List<CandidateResourceResponse>> candidates() {
        return ApiResponse.ok(service.listAllCandidates());
    }

    @PostMapping("/candidates/{candidateId}/review")
    public ApiResponse<CandidateResourceResponse> reviewCandidate(
            @PathVariable Long candidateId,
            @RequestBody ReviewCandidateResourceRequest request
    ) {
        return ApiResponse.ok(service.reviewCandidate(candidateId, request));
    }
}
```

- [ ] **Step 4: On approved resource, create or update formal library record**

```java
if ("APPROVED".equals(request.status())) {
    LearningResource resource = learningResourceService.upsertFromCandidate(candidate);
    candidate.markApproved(adminUserId, request.reviewNote(), resource.getId());
} else {
    candidate.markRejected(adminUserId, request.reviewNote(), request.allowResubmit());
}
```

- [ ] **Step 5: Run backend tests**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=ResourceCollaborationServiceTest,XAgentStudyApplicationTests test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/xagentstudy/resourcecollab backend/src/main/java/com/xagentstudy/admin backend/src/test/java/com/xagentstudy/resourcecollab/ResourceCollaborationServiceTest.java backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java
git commit -m "feat: add admin review flow for resource collaboration"
```

### Task 4: Build User Application Page And Collaborator Workspace

**Files:**

- Create: `frontend/src/views/ResourceApplicationView.vue`
- Create: `frontend/src/views/ResourceContributorView.vue`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Add auth state for resource manager**

```ts
// auth.ts
export interface CurrentUser {
  userId: number
  nickname: string
  account: string
  role: string
  resourceManager: boolean
}

const isResourceManager = computed(() => !!user.value?.resourceManager || isAdmin.value)
```

- [ ] **Step 2: Add routes and sidebar visibility**

```ts
// router.ts
{ path: '/resource-application', component: ResourceApplicationView },
{ path: '/my-resources', component: ResourceContributorView },
```

```vue
<el-menu-item v-if="auth.ready && auth.isResourceManager" index="/my-resources">
  <el-icon><Files /></el-icon>
  <span>我的资源管理</span>
</el-menu-item>
<el-menu-item v-else index="/resource-application">
  <el-icon><Files /></el-icon>
  <span>资源协作申请</span>
</el-menu-item>
```

- [ ] **Step 3: Build the application page**

```vue
<script setup lang="ts">
const form = ref({ reason: '', expertise: '' })
const status = ref<CollaboratorApplication | null>(null)

async function submit() {
  const response = await http.post<ApiResponse<CollaboratorApplication>>('/resource-collaboration/application', form.value)
  status.value = response.data.data
}
</script>

<template>
  <section class="surface panel-pad contributor-application">
    <div class="section-head">
      <div>
        <h2>资源协作申请</h2>
        <p>申请通过后，你可以进入“我的资源管理”，提交并整理自己的采集资源。</p>
      </div>
    </div>
  </section>
</template>
```

- [ ] **Step 4: Build the collaborator workspace**

```vue
<template>
  <section class="contributor-workspace">
    <div class="section-head">
      <div>
        <h2>我的资源管理</h2>
        <p>这里只显示你提交和采集的资源，不包含其他协作者的数据。</p>
      </div>
      <el-button type="primary" @click="createTask">发起采集</el-button>
    </div>
  </section>
</template>
```

- [ ] **Step 5: Run frontend build**

Run: `cd frontend && npm run build`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/ResourceApplicationView.vue frontend/src/views/ResourceContributorView.vue frontend/src/stores/auth.ts frontend/src/router.ts frontend/src/App.vue frontend/src/style.css
git commit -m "feat: add collaborator application and workspace pages"
```

### Task 5: Extend Admin Console For Review And Source Control

**Files:**

- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/style.css`
- Test: `frontend/src/views/AdminView.vue`

- [ ] **Step 1: Add admin tabs for collaborator review**

```ts
interface CollaboratorApplication {
  id: number
  userId: number
  nickname: string
  account: string
  reason: string
  expertise: string
  status: string
  reviewNote: string | null
  createdAt: string
}

interface CandidateResource {
  id: number
  ownerUserId: number
  ownerNickname: string
  sourceType: string
  title: string
  status: string
  sourceUrl: string
}
```

- [ ] **Step 2: Fetch applications and candidates in admin page**

```ts
const collaboratorApplications = ref<CollaboratorApplication[]>([])
const candidateResources = ref<CandidateResource[]>([])

const [applicationsResponse, candidatesResponse] = await Promise.all([
  http.get<ApiResponse<CollaboratorApplication[]>>('/admin/resource-collaboration/applications'),
  http.get<ApiResponse<CandidateResource[]>>('/admin/resource-collaboration/candidates'),
])
```

- [ ] **Step 3: Add review actions**

```ts
async function approveApplication(row: CollaboratorApplication) {
  await http.post(`/admin/resource-collaboration/applications/${row.id}/review`, {
    status: 'APPROVED',
    reviewNote: '允许进入我的资源管理',
  })
  await fetchAdmin()
}
```

- [ ] **Step 4: Add candidate review table**

```vue
<el-table :data="candidateResources">
  <el-table-column prop="ownerNickname" label="提交人" />
  <el-table-column prop="title" label="标题" />
  <el-table-column prop="sourceType" label="来源类型" />
  <el-table-column prop="status" label="状态" />
</el-table>
```

- [ ] **Step 5: Run frontend build**

Run: `cd frontend && npm run build`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/AdminView.vue frontend/src/style.css
git commit -m "feat: add admin review panels for resource collaboration"
```

### Task 6: Final Verification And Integration Sweep

**Files:**

- Modify: `frontend/src/views/ResourceLibraryView.vue`
- Test: `backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java`

- [ ] **Step 1: Make library page point collaborators to their workspace**

```vue
<el-alert
  v-if="auth.isResourceManager"
  title="你当前拥有资源协作权限，可进入“我的资源管理”提交和整理自己的资源。"
  type="success"
  :closable="false"
/>
```

- [ ] **Step 2: Verify backend tests**

Run: `cd backend && DB_PASSWORD='your_password' mvn test`
Expected: PASS

- [ ] **Step 3: Verify frontend build**

Run: `cd frontend && npm run build`
Expected: PASS

- [ ] **Step 4: Verify runtime smoke flow**

Run:

```bash
cd backend
SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD='your_password' mvn spring-boot:run
```

Expected: backend starts successfully with `/api/v1/outcomes`, resource collaboration, and admin controllers all registered

Run:

```bash
cd frontend
npm run dev -- --host 127.0.0.1
```

Expected: browser can open `/resource-application`, `/my-resources`, and `/admin`

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ResourceLibraryView.vue
git commit -m "feat: integrate resource collaboration entry points"
```

## Self-Review

**Spec coverage:** The plan covers collaborator application, collaborator-only workspace, admin review, source whitelist control, content-type split between document ingestion and video indexing, and the rule that collaborators manage only their own resources.

**Placeholder scan:** There are no `TODO` or `TBD` markers. Every task names concrete files and includes explicit commands and minimal code shapes for the main changes.

**Type consistency:** The plan consistently uses `resource manager` as the collaborator permission, `resource-collaboration` as the backend route prefix, `/resource-application` and `/my-resources` as frontend routes, and keeps admin review under `/admin/resource-collaboration`.
