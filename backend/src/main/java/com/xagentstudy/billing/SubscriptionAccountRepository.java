package com.xagentstudy.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionAccountRepository extends JpaRepository<SubscriptionAccount, Long> {
    Optional<SubscriptionAccount> findByUserId(Long userId);
}
