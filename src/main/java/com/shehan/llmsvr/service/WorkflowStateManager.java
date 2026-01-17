package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionContext;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowStateManager {

    private final Map<String, SuspendedState> storage = new ConcurrentHashMap<>();

    public void save(String runId, WorkflowDefinition wf, ExecutionContext ctx) {
        storage.put(runId, new SuspendedState(wf, ctx));
    }

    public SuspendedState get(String runId) {
        return storage.remove(runId);
    }

    @Data
    @AllArgsConstructor
    public static class SuspendedState {
        private final WorkflowDefinition wf;
        private final ExecutionContext ctx;
    }
}
