package com.xagentstudy.agent.task;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskServiceTest {
    @Test
    void listsRecentPlanTasksAsResponses() {
        AgentTaskRepository repository = mock(AgentTaskRepository.class);
        AgentTaskService service = new AgentTaskService(repository);
        AgentTask first = new AgentTask(11L, "QUIZ_AGENT", "{\"count\":5}");
        AgentTask second = new AgentTask(11L, "QA_AGENT", "{\"question\":\"Redis\"}");
        when(repository.findTop20ByPlanIdOrderByUpdatedAtDesc(11L)).thenReturn(List.of(first, second));

        List<AgentTaskResponse> responses = service.listByPlan(11L);

        assertThat(responses).extracting(AgentTaskResponse::taskType)
                .containsExactly("QUIZ_AGENT", "QA_AGENT");
        verify(repository).findTop20ByPlanIdOrderByUpdatedAtDesc(11L);
    }
}
