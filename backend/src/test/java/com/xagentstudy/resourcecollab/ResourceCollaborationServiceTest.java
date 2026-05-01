package com.xagentstudy.resourcecollab;

import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.resource.LearningResource;
import com.xagentstudy.resource.LearningResourceRepository;
import com.xagentstudy.resourcecollab.response.CollaboratorApplicationResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorWorkspaceResponse;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCollaborationServiceTest {
    @Mock
    private CollaboratorApplicationRepository collaboratorApplicationRepository;
    @Mock
    private ResourceManagerGrantRepository resourceManagerGrantRepository;
    @Mock
    private IngestionSourceRepository ingestionSourceRepository;
    @Mock
    private IngestionTaskRepository ingestionTaskRepository;
    @Mock
    private CandidateResourceRepository candidateResourceRepository;
    @Mock
    private LearningResourceRepository learningResourceRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private ResourceCollaborationService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitsApplicationAndReturnsPendingStatus() {
        mockCurrentUser(7L, "USER");
        AppUser user = new AppUser("Alice", "alice@example.com", "hash", "USER");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(collaboratorApplicationRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(collaboratorApplicationRepository.save(any(CollaboratorApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollaboratorApplicationResponse response = service.submitApplication(
                new CollaboratorApplicationRequest("我擅长整理 Java 资料", "Java 后端, Redis")
        );

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.reason()).contains("Java");
        verify(collaboratorApplicationRepository).save(any(CollaboratorApplication.class));
    }

    @Test
    void approvingApplicationGrantsResourceManagerPermission() throws Exception {
        CollaboratorApplication application = new CollaboratorApplication(7L, "申请加入", "Java");
        setField(application, "id", 15L);
        AppUser user = new AppUser("Alice", "alice@example.com", "hash", "USER");
        AppUser admin = new AppUser("Admin", "admin@example.com", "hash", "ADMIN");
        setField(user, "id", 7L);
        setField(admin, "id", 99L);

        when(collaboratorApplicationRepository.findById(15L)).thenReturn(Optional.of(application));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(resourceManagerGrantRepository.findByUserIdAndActiveTrue(7L)).thenReturn(Optional.empty());
        when(resourceManagerGrantRepository.save(any(ResourceManagerGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollaboratorApplicationResponse response = service.reviewApplication(
                15L,
                99L,
                new ReviewCollaboratorApplicationRequest("APPROVED", "通过")
        );

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(user.isResourceManager()).isTrue();

        ArgumentCaptor<ResourceManagerGrant> grantCaptor = ArgumentCaptor.forClass(ResourceManagerGrant.class);
        verify(resourceManagerGrantRepository).save(grantCaptor.capture());
        assertThat(grantCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(grantCaptor.getValue().isActive()).isTrue();
    }

    @Test
    void collaboratorWorkspaceOnlyReturnsOwnResources() throws Exception {
        mockCurrentUser(7L, "USER");
        AppUser user = new AppUser("Alice", "alice@example.com", "hash", "USER");
        setField(user, "id", 7L);
        user.setResourceManager(true);

        IngestionSource ownedSource = new IngestionSource(7L, "Spring Docs", "DOCUMENT", "https://spring.io", true, "OFFICIAL_DOCS");
        setField(ownedSource, "id", 21L);
        IngestionTask ownedTask = new IngestionTask(7L, 21L, "DOCUMENT", "https://spring.io/reference", "PENDING");
        setField(ownedTask, "id", 31L);
        CandidateResource ownedCandidate = new CandidateResource(
                7L,
                21L,
                31L,
                "DOCUMENT",
                "Spring Core",
                "摘要",
                "https://spring.io/reference/core.html",
                "Java,Spring",
                "DRAFT",
                "FULL_TEXT",
                "正文"
        );
        setField(ownedCandidate, "id", 41L);
        LearningResource approvedResource = new LearningResource(
                7L,
                "正式资源",
                "desc",
                "DOCUMENT",
                "spring-core.md",
                "text/markdown",
                "/tmp/spring-core.md",
                "Java",
                "Backend",
                "Spring",
                12L
        );
        setField(approvedResource, "id", 51L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(ingestionSourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(7L)).thenReturn(List.of(ownedSource));
        when(ingestionTaskRepository.findByOwnerUserIdOrderByUpdatedAtDesc(7L)).thenReturn(List.of(ownedTask));
        when(candidateResourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(7L)).thenReturn(List.of(ownedCandidate));
        when(learningResourceRepository.findAllByUploaderUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(approvedResource));

        CollaboratorWorkspaceResponse response = service.workspace();

        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).id()).isEqualTo(21L);
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().get(0).id()).isEqualTo(31L);
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().get(0).id()).isEqualTo(41L);
        assertThat(response.publishedResources()).hasSize(1);
        assertThat(response.publishedResources().get(0).id()).isEqualTo(51L);
    }

    @Test
    void nonCollaboratorCannotAccessWorkspace() {
        mockCurrentUser(8L, "USER");
        AppUser user = new AppUser("Bob", "bob@example.com", "hash", "USER");
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.workspace())
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void adminCanAccessWorkspaceWithoutResourceManagerFlag() throws Exception {
        mockCurrentUser(9L, "ADMIN");
        AppUser admin = new AppUser("Admin", "admin@example.com", "hash", "ADMIN");
        setField(admin, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(ingestionSourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(9L)).thenReturn(List.of());
        when(ingestionTaskRepository.findByOwnerUserIdOrderByUpdatedAtDesc(9L)).thenReturn(List.of());
        when(candidateResourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(9L)).thenReturn(List.of());
        when(learningResourceRepository.findAllByUploaderUserIdOrderByCreatedAtDesc(9L)).thenReturn(List.of());

        CollaboratorWorkspaceResponse response = service.workspace();

        assertThat(response.sources()).isEmpty();
        assertThat(response.tasks()).isEmpty();
        assertThat(response.candidates()).isEmpty();
    }

    private void mockCurrentUser(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "demo", role),
                null,
                List.of()
        ));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
