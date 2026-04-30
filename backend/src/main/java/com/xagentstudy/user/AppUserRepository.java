package com.xagentstudy.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByAccount(String account);

    boolean existsByAccount(String account);
}
