package com.xagentstudy.resourcecollab;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.resource.LearningResource;
import com.xagentstudy.resource.LearningResourceRepository;
import com.xagentstudy.resource.LearningResourceResponse;
import com.xagentstudy.resourcecollab.response.CandidateResourceResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorApplicationResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorWorkspaceResponse;
import com.xagentstudy.resourcecollab.response.IngestionSourceResponse;
import com.xagentstudy.resourcecollab.response.IngestionTaskResponse;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceCollaborationService {
    private final CollaboratorApplicationRepository collaboratorApplicationRepository;
    private final ResourceManagerGrantRepository resourceManagerGrantRepository;
    private final IngestionSourceRepository ingestionSourceRepository;
    private final IngestionTaskRepository ingestionTaskRepository;
    private final CandidateResourceRepository candidateResourceRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final AppUserRepository userRepository;

    public ResourceCollaborationService(
            CollaboratorApplicationRepository collaboratorApplicationRepository,
            ResourceManagerGrantRepository resourceManagerGrantRepository,
            IngestionSourceRepository ingestionSourceRepository,
            IngestionTaskRepository ingestionTaskRepository,
            CandidateResourceRepository candidateResourceRepository,
            LearningResourceRepository learningResourceRepository,
            AppUserRepository userRepository
    ) {
        this.collaboratorApplicationRepository = collaboratorApplicationRepository;
        this.resourceManagerGrantRepository = resourceManagerGrantRepository;
        this.ingestionSourceRepository = ingestionSourceRepository;
        this.ingestionTaskRepository = ingestionTaskRepository;
        this.candidateResourceRepository = candidateResourceRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CollaboratorApplicationResponse currentApplication() {
        return collaboratorApplicationRepository.findByUserId(AuthContext.currentUserId())
                .map(CollaboratorApplicationResponse::from)
                .orElse(null);
    }

    @Transactional
    public CollaboratorApplicationResponse submitApplication(CollaboratorApplicationRequest request) {
        Long userId = AuthContext.currentUserId();
        AppUser user = findUser(userId);
        if (user.isResourceManager()) {
            throw new BusinessException("ALREADY_RESOURCE_MANAGER", "User already has resource collaborator permission");
        }
        CollaboratorApplication application = collaboratorApplicationRepository.findByUserId(userId)
                .map(existing -> reuseApplication(existing, request))
                .orElseGet(() -> new CollaboratorApplication(userId, request.reason().trim(), trimToNull(request.expertise())));
        return CollaboratorApplicationResponse.from(collaboratorApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public CollaboratorWorkspaceResponse workspace() {
        AppUser user = requireResourceManager();
        Long userId = user.getId();
        return new CollaboratorWorkspaceResponse(
                ingestionSourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(userId)
                        .stream()
                        .map(IngestionSourceResponse::from)
                        .toList(),
                ingestionTaskRepository.findByOwnerUserIdOrderByUpdatedAtDesc(userId)
                        .stream()
                        .map(IngestionTaskResponse::from)
                        .toList(),
                candidateResourceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(userId)
                        .stream()
                        .map(CandidateResourceResponse::from)
                        .toList(),
                learningResourceRepository.findAllByUploaderUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(LearningResourceResponse::from)
                        .toList()
        );
    }

    @Transactional
    public IngestionSourceResponse createSource(CreateIngestionSourceRequest request) {
        AppUser user = requireResourceManager();
        IngestionSource source = new IngestionSource(
                user.getId(),
                request.name().trim(),
                normalizeSourceType(request.sourceType()),
                request.baseUrl().trim(),
                true,
                trimToNull(request.sourceCategory())
        );
        return IngestionSourceResponse.from(ingestionSourceRepository.save(source));
    }

    @Transactional
    public IngestionTaskResponse createTask(CreateIngestionTaskRequest request) {
        AppUser user = requireResourceManager();
        IngestionSource source = ingestionSourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Ingestion source not found"));
        if (!source.getOwnerUserId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "Cannot create tasks for another collaborator");
        }

        IngestionTask task = new IngestionTask(user.getId(), source.getId(), source.getSourceType(), request.targetUrl().trim(), "PENDING");
        task.enrich(request.title().trim(), trimToNull(request.summary()), trimToNull(request.tags()));
        task.markCompleted();
        task = ingestionTaskRepository.save(task);

        CandidateResource candidate = new CandidateResource(
                user.getId(),
                source.getId(),
                task.getId(),
                source.getSourceType(),
                request.title().trim(),
                trimToNull(request.summary()),
                request.targetUrl().trim(),
                trimToNull(request.tags()),
                "DRAFT",
                isVideo(source.getSourceType()) ? "INDEX_ONLY" : "FULL_TEXT",
                isVideo(source.getSourceType()) ? null : trimToNull(request.rawContent())
        );
        candidate.update(
                request.title().trim(),
                trimToNull(request.summary()),
                trimToNull(request.tags()),
                trimToNull(request.authorName()),
                trimToNull(request.coverImageUrl()),
                request.durationSeconds(),
                trimToNull(request.rawContent())
        );
        candidateResourceRepository.save(candidate);
        return IngestionTaskResponse.from(task);
    }

    @Transactional
    public CandidateResourceResponse updateCandidate(Long candidateId, UpdateCandidateResourceRequest request) {
        AppUser user = requireResourceManager();
        CandidateResource candidate = findOwnedCandidate(candidateId, user.getId());
        candidate.update(
                request.title().trim(),
                trimToNull(request.summary()),
                trimToNull(request.tags()),
                trimToNull(request.authorName()),
                trimToNull(request.coverImageUrl()),
                request.durationSeconds(),
                trimToNull(request.rawContent())
        );
        return CandidateResourceResponse.from(candidate);
    }

    @Transactional
    public CandidateResourceResponse submitCandidate(Long candidateId) {
        AppUser user = requireResourceManager();
        CandidateResource candidate = findOwnedCandidate(candidateId, user.getId());
        candidate.submitForReview();
        return CandidateResourceResponse.from(candidate);
    }

    @Transactional(readOnly = true)
    public List<CollaboratorApplicationResponse> listApplications() {
        requireAdmin(AuthContext.currentUserId());
        return collaboratorApplicationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(CollaboratorApplicationResponse::from)
                .toList();
    }

    @Transactional
    public CollaboratorApplicationResponse reviewApplication(
            Long applicationId,
            Long reviewerUserId,
            ReviewCollaboratorApplicationRequest request
    ) {
        requireAdmin(reviewerUserId);
        CollaboratorApplication application = collaboratorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Collaborator application not found"));
        String status = normalizeApplicationStatus(request.status());
        application.review(status, trimToNull(request.reviewNote()), reviewerUserId);
        if ("APPROVED".equals(status)) {
            AppUser user = findUser(application.getUserId());
            user.setResourceManager(true);
            if (resourceManagerGrantRepository.findByUserIdAndActiveTrue(user.getId()).isEmpty()) {
                resourceManagerGrantRepository.save(new ResourceManagerGrant(user.getId(), reviewerUserId, trimToNull(request.reviewNote())));
            }
        }
        return CollaboratorApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<IngestionSourceResponse> adminSources() {
        requireAdmin(AuthContext.currentUserId());
        return ingestionSourceRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(IngestionSourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateResourceResponse> adminCandidates() {
        requireAdmin(AuthContext.currentUserId());
        return candidateResourceRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(CandidateResourceResponse::from)
                .toList();
    }

    @Transactional
    public CandidateResourceResponse reviewCandidate(Long candidateId, ReviewCandidateResourceRequest request) {
        AppUser reviewer = requireAdmin(AuthContext.currentUserId());
        CandidateResource candidate = candidateResourceRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Candidate resource not found"));
        String status = normalizeCandidateStatus(request.status());
        candidate.review(status, trimToNull(request.reviewNote()), reviewer.getId());
        if ("APPROVED".equals(status)) {
            LearningResource resource = learningResourceRepository.save(toLearningResource(candidate));
            candidate.markPublished(resource.getId());
        }
        return CandidateResourceResponse.from(candidate);
    }

    private CollaboratorApplication reuseApplication(CollaboratorApplication application, CollaboratorApplicationRequest request) {
        if ("APPROVED".equals(application.getStatus())) {
            throw new BusinessException("APPLICATION_ALREADY_APPROVED", "Application already approved");
        }
        if ("PENDING".equals(application.getStatus())) {
            throw new BusinessException("APPLICATION_PENDING", "Application is still pending");
        }
        application.resubmit(request.reason().trim(), trimToNull(request.expertise()));
        return application;
    }

    private LearningResource toLearningResource(CandidateResource candidate) {
        String description = candidate.isVideo()
                ? trimToNull(candidate.getSummary())
                : trimToNull(firstNonBlank(candidate.getSummary(), candidate.getRawContent()));
        String originalFilename = candidate.isVideo()
                ? candidate.getTitle() + ".url"
                : candidate.getTitle() + ".md";
        String contentType = candidate.isVideo() ? "text/uri-list" : "text/markdown";
        String storageKey = candidate.getResourceUrl();
        String subjectName = candidate.isVideo() ? "Video" : "Document";
        String subjectScope = candidate.getSourceType();
        String tags = firstNonBlank(candidate.getTags(), candidate.getSourceType());
        return new LearningResource(
                candidate.getOwnerUserId(),
                candidate.getTitle(),
                description,
                candidate.isVideo() ? "VIDEO" : "DOCUMENT",
                originalFilename,
                contentType,
                storageKey,
                subjectName,
                subjectScope,
                tags,
                0L
        );
    }

    private CandidateResource findOwnedCandidate(Long candidateId, Long ownerUserId) {
        CandidateResource candidate = candidateResourceRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Candidate resource not found"));
        if (!candidate.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("FORBIDDEN", "Cannot access another collaborator's resource");
        }
        return candidate;
    }

    private AppUser requireResourceManager() {
        AppUser user = findUser(AuthContext.currentUserId());
        if (!user.isResourceManager() && !user.isAdmin()) {
            throw new BusinessException("FORBIDDEN", "Resource collaborator permission required");
        }
        return user;
    }

    private AppUser requireAdmin(Long userId) {
        AppUser user = findUser(userId);
        if (!user.isAdmin()) {
            throw new BusinessException("FORBIDDEN", "Admin permission required");
        }
        return user;
    }

    private AppUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
    }

    private String normalizeApplicationStatus(String status) {
        String normalized = firstNonBlank(status, "").trim().toUpperCase();
        if (!List.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "Unsupported application review status");
        }
        return normalized;
    }

    private String normalizeCandidateStatus(String status) {
        String normalized = firstNonBlank(status, "").trim().toUpperCase();
        if (!List.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "Unsupported candidate review status");
        }
        return normalized;
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = firstNonBlank(sourceType, "").trim().toUpperCase();
        if (!List.of("DOCUMENT", "VIDEO").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "Unsupported source type");
        }
        return normalized;
    }

    private boolean isVideo(String sourceType) {
        return "VIDEO".equalsIgnoreCase(sourceType);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
