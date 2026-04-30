package com.xagentstudy.knowledge;

import com.xagentstudy.agent.task.AgentTaskResponse;

public record DocumentUploadResponse(
        DocumentResponse document,
        AgentTaskResponse task
) {
}
