package com.xagentstudy.resourcecollab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateResourceRepository extends JpaRepository<CandidateResource, Long> {
    List<CandidateResource> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);

    List<CandidateResource> findAllByOrderByUpdatedAtDesc();
}
