package com.xagentstudy.modelconfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {
    List<ModelConfig> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ModelConfig> findFirstByUserIdAndEnabledTrueOrderByUpdatedAtDesc(Long userId);

    Optional<ModelConfig> findFirstByUserIdInAndEnabledTrueOrderByUpdatedAtDesc(List<Long> userIds);
}
