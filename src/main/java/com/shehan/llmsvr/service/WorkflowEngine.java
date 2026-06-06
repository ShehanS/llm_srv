package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@WorkflowInterface
public interface WorkflowEngine {
    @WorkflowMethod
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf, String runId);

    public Flux<ExecutionTrace> getTrace(String runId);
    @WorkflowMethod
    public Mono<String> resume(String runId, MessageBatch humanInput, String outputHandle);
    public Flux<ExecutionTrace> liveTrace(String runId);

    Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId);

    @WorkflowMethod
    Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String runId);

    @WorkflowMethod
    public Mono<String> runMultipleNodes(MessageBatch batch, WorkflowDefinition wf, List<String> nodeIds, String flowId);

}
