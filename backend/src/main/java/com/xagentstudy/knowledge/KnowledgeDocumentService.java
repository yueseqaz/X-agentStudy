package com.xagentstudy.knowledge;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.AgentGenerationService.KnowledgePointContext;
import com.xagentstudy.agent.orchestration.AgentGenerationService.LearningPlanContext;
import com.xagentstudy.agent.task.AgentTask;
import com.xagentstudy.agent.task.AgentTaskResponse;
import com.xagentstudy.agent.task.AgentTaskService;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.profile.LearningProfile;
import com.xagentstudy.profile.LearningProfileRepository;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import com.xagentstudy.summary.SummaryCardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class KnowledgeDocumentService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningProfileRepository profileRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final AgentTaskService taskService;
    private final KnowledgeChunkService chunkService;
    private final DocumentParsingService parsingService;
    private final AgentGenerationService agentGenerationService;
    private final BillingService billingService;
    private final SummaryCardService summaryCardService;
    private final Path uploadRoot;

    public KnowledgeDocumentService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            LearningProfileRepository profileRepository,
            KnowledgeDocumentRepository documentRepository,
            AgentTaskService taskService,
            KnowledgeChunkService chunkService,
            DocumentParsingService parsingService,
            AgentGenerationService agentGenerationService,
            BillingService billingService,
            SummaryCardService summaryCardService,
            @Value("${app.storage.upload-root:uploads}") String uploadRoot
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.profileRepository = profileRepository;
        this.documentRepository = documentRepository;
        this.taskService = taskService;
        this.chunkService = chunkService;
        this.parsingService = parsingService;
        this.agentGenerationService = agentGenerationService;
        this.billingService = billingService;
        this.summaryCardService = summaryCardService;
        this.uploadRoot = Path.of(uploadRoot);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long planId) {
        ensurePlan(planId);
        return documentRepository.findByPlanIdOrderByUploadedAtDesc(planId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse detail(Long planId, Long documentId) {
        ensurePlan(planId);
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(item -> item.getPlanId().equals(planId))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Document not found"));
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse content(Long planId, Long documentId) {
        ensurePlan(planId);
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(item -> item.getPlanId().equals(planId))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Document not found"));
        try {
            return new DocumentContentResponse(DocumentResponse.from(document), Files.readString(Path.of(document.getStorageKey()), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BusinessException("DOCUMENT_READ_FAILED", "Unable to read document content");
        }
    }

    @Transactional
    public DocumentUploadResponse upload(Long planId, MultipartFile file) {
        ensurePlan(planId);
        if (file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Uploaded file is empty");
        }

        String originalName = file.getOriginalFilename() == null ? "untitled" : file.getOriginalFilename();
        String type = detectType(originalName);
        validateType(type);
        billingService.ensureStorageAvailable(file.getSize());

        try {
            Path planDir = uploadRoot.resolve("plans").resolve(String.valueOf(planId));
            Files.createDirectories(planDir);
            String storedName = UUID.randomUUID() + "-" + sanitizeName(originalName);
            Path target = planDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument saved = documentRepository.save(new KnowledgeDocument(
                    planId,
                    originalName,
                    type,
                    target.toString()
            ));
            AgentTask task = taskService.create(
                    planId,
                    "DOCUMENT_PARSE",
                    "{\"documentId\":" + saved.getId() + ",\"filename\":\"" + escapeJson(originalName) + "\"}"
            );
            scheduleParsing(saved.getId(), task.getId());
            return new DocumentUploadResponse(DocumentResponse.from(saved), AgentTaskResponse.from(task));
        } catch (IOException ex) {
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", "Failed to store uploaded file");
        }
    }

    @Transactional
    public GeneratedOnlineDocumentResponse generateOnlineDocument(Long planId, GenerateOnlineDocumentRequest request) {
        LearningPlan plan = ensurePlan(planId);
        LearningDirection direction = directionRepository.findById(plan.getDirectionId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
        LearningProfile profile = plan.getProfileId() == null
                ? profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(plan.getDirectionId()).orElse(null)
                : profileRepository.findById(plan.getProfileId()).orElse(null);

        String title = request.title() == null || request.title().isBlank() ? "在线学习文档" : request.title().trim();
        String markdown = agentGenerationService.generateKnowledgeDocumentMarkdown(
                direction,
                profile,
                new LearningPlanContext(plan.getTitle(), plan.getGoal()),
                new KnowledgePointContext(
                        blankToDefault(request.knowledgePointId(), "kp-" + UUID.randomUUID()),
                        title,
                        blankToDefault(request.chapterName(), "未命名章节"),
                        blankToDefault(request.unitName(), "未命名单元"),
                        blankToDefault(request.level(), "FOUNDATION"),
                        blankToDefault(request.outcome(), "完成该知识点学习")
                ),
                blankToDefault(request.documentStyle(), "PROFESSIONAL")
        );
        billingService.ensureStorageAvailable(markdown.getBytes(StandardCharsets.UTF_8).length);

        try {
            Path generatedDir = uploadRoot.resolve("plans").resolve(String.valueOf(planId)).resolve("generated");
            Files.createDirectories(generatedDir);
            String filename = sanitizeName(title) + "-" + UUID.randomUUID() + ".md";
            Path target = generatedDir.resolve(filename);
            Files.writeString(target, markdown, StandardCharsets.UTF_8);

            KnowledgeDocument saved = documentRepository.save(new KnowledgeDocument(
                    planId,
                    "在线生成 - " + title + ".md",
                    "MD",
                    target.toString()
            ));
            parsingService.parseGeneratedContent(saved.getId(), markdown);
            KnowledgeDocument parsed = documentRepository.findById(saved.getId()).orElse(saved);
            return new GeneratedOnlineDocumentResponse(DocumentResponse.from(parsed), markdown);
        } catch (IOException ex) {
            throw new BusinessException("DOCUMENT_GENERATION_FAILED", "Failed to store generated document");
        }
    }

    @Transactional
    public void delete(Long planId, Long documentId) {
        ensurePlan(planId);
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(item -> item.getPlanId().equals(planId))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Document not found"));
        chunkService.deleteByDocument(documentId);
        summaryCardService.deleteByDocument(documentId);
        documentRepository.delete(document);
        try {
            Files.deleteIfExists(Path.of(document.getStorageKey()));
        } catch (IOException ignored) {
            // Metadata deletion should not fail if a local file was already removed.
        }
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private String detectType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "UNKNOWN";
        }
        return filename.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private void validateType(String type) {
        if (!List.of("PDF", "PPT", "PPTX", "MD", "MARKDOWN", "TXT", "DOCX").contains(type)) {
            throw new BusinessException("UNSUPPORTED_DOCUMENT_TYPE", "Unsupported document type: " + type);
        }
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void scheduleParsing(Long documentId, Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    parsingService.parseAsync(documentId, taskId);
                }
            });
        } else {
            parsingService.parseAsync(documentId, taskId);
        }
    }
}
