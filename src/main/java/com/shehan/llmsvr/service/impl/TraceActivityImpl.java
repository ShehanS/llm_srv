package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraceActivityImpl implements WorkflowOrchestrator.TraceActivity {

    private final WorkflowEngineImpl workflowEngine;

    @Override
    public void emitTrace(ExecutionTrace trace) {

        workflowEngine.receiveExternalTraceUpdate(trace);
    }
}