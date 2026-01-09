package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkflowEngine {
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf);

    public Flux<ExecutionTrace> getTrace(String runId);

    public Flux<ExecutionTrace> liveTrace(String runId);

    Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId);

    Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId);

}
