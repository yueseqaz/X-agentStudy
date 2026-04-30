package com.xagentstudy.agent.task;

import com.xagentstudy.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentTaskService {
    private final AgentTaskRepository repository;

    public AgentTaskService(AgentTaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AgentTask create(Long planId, String taskType, String inputPayload) {
        return repository.save(new AgentTask(planId, taskType, inputPayload));
    }

    @Transactional
    public AgentTask markRunning(Long taskId) {
        AgentTask task = getTask(taskId);
        task.markRunning();
        return task;
    }

    @Transactional
    public AgentTask markSuccess(Long taskId, String outputPayload) {
        AgentTask task = getTask(taskId);
        task.markSuccess(outputPayload);
        return task;
    }

    @Transactional
    public AgentTask markFailed(Long taskId, String errorMessage) {
        AgentTask task = getTask(taskId);
        task.markFailed(errorMessage);
        return task;
    }

    @Transactional(readOnly = true)
    public AgentTaskResponse get(Long taskId) {
        return AgentTaskResponse.from(getTask(taskId));
    }

    private AgentTask getTask(Long taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Agent task not found"));
    }
}
