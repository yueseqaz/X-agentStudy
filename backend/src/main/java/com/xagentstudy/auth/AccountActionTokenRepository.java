package com.xagentstudy.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {
    Optional<AccountActionToken> findByTokenHashAndActionType(String tokenHash, String actionType);
}
