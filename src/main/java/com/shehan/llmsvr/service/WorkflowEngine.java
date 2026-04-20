package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface WorkflowEngine {
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf, String runId);

    public Flux<ExecutionTrace> getTrace(String runId);
    public Mono<String> resume(String runId, MessageBatch humanInput, String outputHandle);
    public Flux<ExecutionTrace> liveTrace(String runId);

    Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId);

    Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String runId);

    public Mono<String> runMultipleNodes(MessageBatch batch, WorkflowDefinition wf, List<String> nodeIds, String flowId);

}
