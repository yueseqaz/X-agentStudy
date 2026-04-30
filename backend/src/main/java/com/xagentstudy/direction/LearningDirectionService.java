package com.xagentstudy.direction;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.LearningPlanService;
import com.xagentstudy.profile.LearningProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LearningDirectionService {
    private final LearningDirectionRepository repository;
    private final LearningPlanRepository planRepository;
    private final LearningPlanService planService;
    private final LearningProfileRepository profileRepository;
    private final BillingService billingService;

    public LearningDirectionService(
            LearningDirectionRepository repository,
            LearningPlanRepository planRepository,
            LearningPlanService planService,
            LearningProfileRepository profileRepository,
            BillingService billingService
    ) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.planService = planService;
        this.profileRepository = profileRepository;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public List<DirectionResponse> list() {
        return repository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(AuthContext.currentUserId())
                .stream()
                .map(DirectionResponse::from)
                .toList();
    }

    @Transactional
    public DirectionResponse create(CreateDirectionRequest request) {
        billingService.ensurePlanSlotAvailable();
        LearningDirection saved = repository.save(new LearningDirection(
                AuthContext.currentUserId(),
                request.name(),
                request.category(),
                request.description()
        ));
        return DirectionResponse.from(saved);
    }

    @Transactional
    public DirectionResponse update(Long id, CreateDirectionRequest request) {
        LearningDirection direction = repository.findById(id)
                .filter(item -> item.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
        direction.update(request.name(), request.category(), request.description());
        return DirectionResponse.from(direction);
    }

    @Transactional
    public void delete(Long id) {
        LearningDirection direction = repository.findById(id)
                .filter(item -> item.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
        List<Long> planIds = planRepository.findByDirectionIdOrderByCreatedAtDesc(direction.getId())
                .stream()
                .map(LearningPlan::getId)
                .toList();
        for (Long planId : planIds) {
            planService.delete(planId);
        }
        profileRepository.deleteByDirectionId(direction.getId());
        repository.delete(direction);
    }
}
