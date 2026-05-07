package com.xagentstudy.agent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    long countByStatus(String status);

    List<AgentTask> findTop12ByOrderByUpdatedAtDesc();

    List<AgentTask> findTop8ByStatusOrderByUpdatedAtDesc(String status);

    List<AgentTask> findTop20ByPlanIdOrderByUpdatedAtDesc(Long planId);

    void deleteByPlanId(Long planId);
}
