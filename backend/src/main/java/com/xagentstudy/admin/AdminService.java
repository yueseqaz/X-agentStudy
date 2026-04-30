package com.xagentstudy.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.task.AgentTaskRepository;
import com.xagentstudy.audit.AuditLogRepository;
import com.xagentstudy.audit.AuditLogService;
import com.xagentstudy.billing.SubscriptionAccount;
import com.xagentstudy.billing.SubscriptionAccountRepository;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.DocumentParsingService;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminService {
    private final AppUserRepository userRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final QuestionRepository questionRepository;
    private final AgentTaskRepository taskRepository;
    private final SubscriptionAccountRepository subscriptionRepository;
    private final DocumentParsingService documentParsingService;
    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final BillingService billingService;

    public AdminService(
            AppUserRepository userRepository,
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            KnowledgeDocumentRepository documentRepository,
            QuestionRepository questionRepository,
            AgentTaskRepository taskRepository,
            SubscriptionAccountRepository subscriptionRepository,
            DocumentParsingService documentParsingService,
            ObjectMapper objectMapper,
            AuditLogRepository auditLogRepository,
            AuditLogService auditLogService,
            BillingService billingService
    ) {
        this.userRepository = userRepository;
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.documentRepository = documentRepository;
        this.questionRepository = questionRepository;
        this.taskRepository = taskRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.documentParsingService = documentParsingService;
        this.objectMapper = objectMapper;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.billingService = billingService;
    }

    public AdminSummaryResponse summary() {
        List<AppUser> users = userRepository.findAll();
        List<SubscriptionAccount> subscriptions = subscriptionRepository.findAll();
        return new AdminSummaryResponse(
                users.size(),
                directionRepository.count(),
                planRepository.count(),
                documentRepository.count(),
                questionRepository.count(),
                taskRepository.count(),
                taskRepository.countByStatus("RUNNING"),
                taskRepository.countByStatus("FAILED"),
                subscriptions.size(),
                users.stream().filter(user -> Boolean.TRUE.equals(user.getDisabled())).count(),
                users.stream().filter(user -> user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())).count(),
                subscriptions.stream().mapToLong(SubscriptionAccount::getUsedAgentCalls).sum(),
                subscriptions.stream().mapToLong(SubscriptionAccount::getMonthlyAgentQuota).sum()
        );
    }

    public List<AdminUserResponse> users() {
        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    public List<AdminTaskResponse> recentTasks() {
        return taskRepository.findTop12ByOrderByUpdatedAtDesc()
                .stream()
                .map(AdminTaskResponse::from)
                .toList();
    }

    public List<AdminTaskResponse> failedTasks() {
        return taskRepository.findTop8ByStatusOrderByUpdatedAtDesc("FAILED")
                .stream()
                .map(AdminTaskResponse::from)
                .toList();
    }

    public List<AdminQuotaResponse> quotas() {
        java.util.Map<Long, AppUser> userMap = userRepository.findAll()
                .stream()
                .collect(java.util.stream.Collectors.toMap(AppUser::getId, user -> user));
        return subscriptionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(SubscriptionAccount::getUsedAgentCalls).reversed())
                .map(account -> AdminQuotaResponse.from(
                        account,
                        userMap.get(account.getUserId()),
                        billingService.usedStorageMbForUser(account.getUserId()),
                        billingService.usedPlanCountForUser(account.getUserId())
                ))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public AdminUserResponse setUserDisabled(Long userId, boolean disabled, OffsetDateTime disabledUntil) {
        if (userId.equals(AuthContext.currentUserId()) && disabled) {
            throw new BusinessException("SELF_DISABLE_FORBIDDEN", "Cannot disable current admin account");
        }
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        user.setDisabled(disabled, disabledUntil);
        auditLogService.record("USER", userId, disabled ? "DISABLE_USER" : "ENABLE_USER",
                disabled ? "disabledUntil=" + disabledUntil : "enabled");
        return AdminUserResponse.from(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public AdminQuotaResponse resetQuota(Long userId) {
        SubscriptionAccount account = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Subscription account not found"));
        account.resetUsage();
        auditLogService.record("SUBSCRIPTION", account.getId(), "RESET_QUOTA", "userId=" + userId);
        AppUser user = userRepository.findById(userId).orElse(null);
        return AdminQuotaResponse.from(
                account,
                user,
                billingService.usedStorageMbForUser(userId),
                billingService.usedPlanCountForUser(userId)
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public AdminQuotaResponse rechargeWallet(Long userId, Integer amountCents, String remark) {
        SubscriptionAccount account = billingService.rechargeWallet(userId, amountCents == null ? 0 : amountCents, remark);
        AppUser user = userRepository.findById(userId).orElse(null);
        return AdminQuotaResponse.from(
                account,
                user,
                billingService.usedStorageMbForUser(userId),
                billingService.usedPlanCountForUser(userId)
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminTaskResponse taskDetail(Long taskId) {
        return taskRepository.findById(taskId)
                .map(AdminTaskResponse::from)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Agent task not found"));
    }

    @org.springframework.transaction.annotation.Transactional
    public AdminTaskResponse retryTask(Long taskId) {
        com.xagentstudy.agent.task.AgentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Agent task not found"));
        if (!"FAILED".equals(task.getStatus())) {
            throw new BusinessException("TASK_NOT_FAILED", "Only failed tasks can be retried");
        }
        if (!"DOCUMENT_PARSE".equals(task.getTaskType())) {
            throw new BusinessException("UNSUPPORTED_RETRY_TASK", "Only document parse tasks can be retried now");
        }
        Long documentId = parseDocumentId(task.getInputPayload());
        documentParsingService.parseAsync(documentId, task.getId());
        auditLogService.record("AGENT_TASK", taskId, "RETRY_TASK", "documentId=" + documentId);
        return AdminTaskResponse.from(task);
    }

    public List<AdminAuditLogResponse> auditLogs() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(AdminAuditLogResponse::from)
                .toList();
    }

    private Long parseDocumentId(String inputPayload) {
        try {
            JsonNode node = objectMapper.readTree(inputPayload);
            if (node.hasNonNull("documentId")) {
                return node.get("documentId").asLong();
            }
        } catch (Exception ignored) {
            // Fall through to a business error with a stable code.
        }
        throw new BusinessException("INVALID_TASK_PAYLOAD", "Cannot find documentId in task payload");
    }
}
