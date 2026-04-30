package com.xagentstudy.billing;

import com.xagentstudy.audit.AuditLogService;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class BillingService {
    private final SubscriptionAccountRepository repository;
    private final AuditLogService auditLogService;
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public BillingService(
            SubscriptionAccountRepository repository,
            AuditLogService auditLogService,
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            KnowledgeDocumentRepository documentRepository
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public BillingResponse current() {
        SubscriptionAccount account = currentAccount();
        return BillingResponse.from(account, usedStorageMb(AuthContext.currentUserId()), usedPlanCount(AuthContext.currentUserId()));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> plans() {
        return Arrays.stream(SubscriptionPlan.values())
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    @Transactional
    public BillingResponse simulateChange(ChangeSubscriptionRequest request) {
        return current();
    }

    @Transactional
    public BillingResponse purchase(ChangeSubscriptionRequest request) {
        if (request.planCode() == null || request.planCode().isBlank()) {
            throw new BusinessException("INVALID_PLAN", "Unsupported subscription plan");
        }
        SubscriptionPlan plan = SubscriptionPlan.fromCode(request.planCode());
        if (!plan.name().equalsIgnoreCase(request.planCode().trim())) {
            throw new BusinessException("INVALID_PLAN", "Unsupported subscription plan");
        }
        SubscriptionAccount account = currentAccount();
        SubscriptionPlan currentPlan = SubscriptionPlan.fromCode(account.getPlanCode());
        if (plan == currentPlan) {
            throw new BusinessException("PLAN_ALREADY_ACTIVE", "当前已经是该套餐，无需重复购买");
        }
        if (plan.lowerThan(currentPlan)) {
            throw new BusinessException("PLAN_DOWNGRADE_FORBIDDEN", "当前套餐等级更高，暂不支持降级购买");
        }
        String oldPlan = account.getPlanCode();
        int payableCents = Math.max(0, plan.monthlyPriceCents() - currentPlan.monthlyPriceCents());
        if (payableCents > 0 && !account.hasWalletBalance(payableCents)) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "Wallet balance is insufficient");
        }
        if (payableCents > 0) {
            account.deductWallet(payableCents);
        }
        account.changePlan(plan);
        auditLogService.record("SUBSCRIPTION", account.getId(), "PURCHASE_SUBSCRIPTION",
                "userId=" + AuthContext.currentUserId()
                        + ", from=" + oldPlan
                        + ", to=" + plan.name()
                        + ", priceCents=" + payableCents);
        return BillingResponse.from(account, usedStorageMb(AuthContext.currentUserId()), usedPlanCount(AuthContext.currentUserId()));
    }

    @Transactional
    public void consumeAgentCall() {
        consumeAgentCall("AGENT_CALL");
    }

    @Transactional
    public void consumeAgentCall(String reason) {
        SubscriptionAccount account = currentAccount();
        if (!account.hasAgentQuota(1)) {
            throw new BusinessException("QUOTA_EXCEEDED", "Agent 调用额度已用完，请升级套餐或等待下个周期");
        }
        account.consumeAgentCalls(1);
        auditLogService.record("SUBSCRIPTION", account.getId(), "CONSUME_AGENT_QUOTA",
                "userId=" + AuthContext.currentUserId() + ", amount=1, reason=" + reason);
    }

    @Transactional
    public SubscriptionAccount rechargeWallet(Long userId, int amountCents, String remark) {
        if (amountCents <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "Recharge amount must be positive");
        }
        SubscriptionAccount account = accountForUser(userId);
        account.rechargeWallet(amountCents);
        auditLogService.record("SUBSCRIPTION", account.getId(), "RECHARGE_WALLET",
                "userId=" + userId + ", amountCents=" + amountCents + ", remark=" + (remark == null ? "" : remark));
        return account;
    }

    @Transactional(readOnly = true)
    public void ensureStorageAvailable(long additionalBytes) {
        // Storage quota is intentionally not enforced in this product version.
    }

    @Transactional(readOnly = true)
    public void ensurePlanSlotAvailable() {
        SubscriptionAccount account = currentAccount();
        int usedPlanCount = usedPlanCount(AuthContext.currentUserId());
        int planQuota = SubscriptionPlan.fromCode(account.getPlanCode()).planQuota();
        if (usedPlanCount >= planQuota) {
            throw new BusinessException("PLAN_QUOTA_EXCEEDED", "学习计划数量已达到当前套餐上限，请升级套餐后继续");
        }
    }

    @Transactional(readOnly = true)
    public int usedStorageMbForUser(Long userId) {
        return usedStorageMb(userId);
    }

    @Transactional(readOnly = true)
    public int usedPlanCountForUser(Long userId) {
        return usedPlanCount(userId);
    }

    @Transactional
    public SubscriptionAccount accountForUser(Long userId) {
        SubscriptionPlan free = SubscriptionPlan.FREE;
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(new SubscriptionAccount(
                        userId,
                        free.name(),
                        free.monthlyAgentQuota(),
                        free.storageQuotaMb()
                )));
    }

    private SubscriptionAccount currentAccount() {
        return accountForUser(AuthContext.currentUserId());
    }

    private int usedStorageMb(Long userId) {
        long bytes = usedStorageBytes(userId);
        if (bytes <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.ceil(bytes / 1024.0 / 1024.0));
    }

    private int usedPlanCount(Long userId) {
        List<Long> directionIds = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId)
                .stream()
                .map(com.xagentstudy.direction.LearningDirection::getId)
                .toList();
        if (directionIds.isEmpty()) {
            return 0;
        }
        return planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds).size();
    }

    private long usedStorageBytes(Long userId) {
        List<Long> directionIds = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId)
                .stream()
                .map(com.xagentstudy.direction.LearningDirection::getId)
                .toList();
        if (directionIds.isEmpty()) {
            return 0;
        }
        List<Long> planIds = planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds)
                .stream()
                .map(com.xagentstudy.plan.LearningPlan::getId)
                .toList();
        if (planIds.isEmpty()) {
            return 0;
        }
        return documentRepository.findByPlanIdIn(planIds)
                .stream()
                .mapToLong(this::documentSizeBytes)
                .sum();
    }

    private long documentSizeBytes(KnowledgeDocument document) {
        try {
            return Files.size(Path.of(document.getStorageKey()));
        } catch (Exception ignored) {
            return Math.max(0, document.getContentLength() == null ? 0 : document.getContentLength());
        }
    }
}
