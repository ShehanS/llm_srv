package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionContext;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Data
public class WorkflowStateManager {

    private final Map<String, SuspendedState> storage = new ConcurrentHashMap<>();

    public void save(String runId, WorkflowDefinition wf, ExecutionContext ctx) {
        storage.put(runId, new SuspendedState(wf, ctx));
    }

    /**
     * Retrieves and removes the state for the given runId.
     * This prevents multiple resume attempts for the same state.
     */
    public SuspendedState getAndRemove(String runId) {
        return storage.remove(runId);
    }

    @Data
    @AllArgsConstructor
    public static class SuspendedState {
        private final WorkflowDefinition wf;
        private final ExecutionContext ctx;
    }
}
