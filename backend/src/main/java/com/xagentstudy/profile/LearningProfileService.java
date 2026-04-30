package com.xagentstudy.profile;

import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.GeneratedProfile;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningProfileService {
    private final LearningDirectionRepository directionRepository;
    private final LearningProfileRepository profileRepository;
    private final AgentGenerationService agentGenerationService;

    public LearningProfileService(
            LearningDirectionRepository directionRepository,
            LearningProfileRepository profileRepository,
            AgentGenerationService agentGenerationService
    ) {
        this.directionRepository = directionRepository;
        this.profileRepository = profileRepository;
        this.agentGenerationService = agentGenerationService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse latest(Long directionId) {
        ensureDirection(directionId);
        return profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(directionId)
                .map(ProfileResponse::from)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning profile not found"));
    }

    @Transactional
    public ProfileResponse generate(Long directionId, GenerateProfileRequest request) {
        LearningDirection direction = ensureDirection(directionId);
        GeneratedProfile generated = agentGenerationService.generateProfile(direction, request);
        String rawConversation = toJsonArray(agentGenerationService.profileAnswers(request));

        LearningProfile saved = profileRepository.save(new LearningProfile(
                directionId,
                generated.goal(),
                generated.currentLevel(),
                generated.timeBudget(),
                generated.preference(),
                generated.risks(),
                generated.strategy(),
                rawConversation
        ));
        return ProfileResponse.from(saved);
    }

    private LearningDirection ensureDirection(Long directionId) {
        return directionRepository.findById(directionId)
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
    }

    private String toJsonArray(String content) {
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "[\"" + escaped + "\"]";
    }
}
