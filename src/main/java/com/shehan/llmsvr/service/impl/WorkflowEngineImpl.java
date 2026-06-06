package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowOrchestrator;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineImpl implements WorkflowEngine {

    private final WorkflowClient workflowClient;
    private static final String TASK_QUEUE = "AI_WORKFLOW_TASK_QUEUE";

    private final Map<String, Sinks.Many<ExecutionTrace>> sinksByRunId = new ConcurrentHashMap<>();

    @Override
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf, String runId) {
        return Mono.fromCallable(() -> {
            String uniqueTemporalId = runId + "-" + UUID.randomUUID().toString().substring(0, 8);
            prepareForRun(runId);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowId(uniqueTemporalId)
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build();

            WorkflowOrchestrator.StandardEngine workflow = workflowClient.newWorkflowStub(WorkflowOrchestrator.StandardEngine.class, options);
            WorkflowClient.start(workflow::runTemporalWorkflow, startMessages, wf, runId);
            return uniqueTemporalId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> resume(String runId, MessageBatch humanInput, String outputHandle) {
        return Mono.fromRunnable(() -> {
            log.info("Dispatching verification signal down to Temporal for execution id: {}", runId);
            WorkflowOrchestrator.StandardEngine workflow = workflowClient.newWorkflowStub(WorkflowOrchestrator.StandardEngine.class, runId);
            workflow.resumeSignal(humanInput, outputHandle);
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(runId);
    }

    @Override
    public Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String runId) {
        return Mono.fromCallable(() -> {
            String uniqueTemporalId = runId + "-" + UUID.randomUUID().toString().substring(0, 8);
            prepareForRun(runId);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowId(uniqueTemporalId)
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build();

            WorkflowOrchestrator.NodeEngine workflow = workflowClient.newWorkflowStub(WorkflowOrchestrator.NodeEngine.class, options);
            WorkflowClient.start(workflow::runTemporalFromNode, batch, wf, startNodeId, runId);
            return uniqueTemporalId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> runMultipleNodes(MessageBatch batch, WorkflowDefinition wf, List<String> nodeIds, String flowId) {
        return Mono.fromCallable(() -> {
            String uniqueTemporalId = flowId + "-" + UUID.randomUUID().toString().substring(0, 8);
            prepareForRun(flowId);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowId(uniqueTemporalId)
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build();

            WorkflowOrchestrator.ParallelEngine workflow = workflowClient.newWorkflowStub(WorkflowOrchestrator.ParallelEngine.class, options);
            WorkflowClient.start(workflow::runTemporalMultipleNodes, batch, wf, nodeIds, flowId);
            return uniqueTemporalId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public void receiveExternalTraceUpdate(ExecutionTrace trace) {
        if (trace != null && trace.getRunId() != null) {
            log.info("receiveExternalTraceUpdate called for runId: {}", trace.getRunId());
            log.info("Available sink keys: {}", sinksByRunId.keySet());
            Sinks.Many<ExecutionTrace> sink = sinksByRunId.get(trace.getRunId());
            if (sink == null) {
                log.warn("No sink found for runId: {}", trace.getRunId());
                return;
            }
            Sinks.EmitResult result = sink.tryEmitNext(trace);
            log.info("Emit result: {}", result);
        }
    }

    @Override
    public Flux<ExecutionTrace> getTrace(String runId) {
        Sinks.Many<ExecutionTrace> sink = sinksByRunId.get(runId);
        if (sink == null) {
            return Flux.empty();
        }
        return sink.asFlux().take(Duration.ofMillis(100)).onErrorResume(e -> Flux.empty());
    }

    @Override
    public Flux<ExecutionTrace> liveTrace(String runId) {
        log.info("liveTrace subscribed for runId: {}", runId);
        log.info("Available sink keys at subscription time: {}", sinksByRunId.keySet());
        return getOrCreateSink(runId).asFlux();
    }
    @Override
    public Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId) {
        return liveTrace(runId).filter(t -> nodeId.equals(t.getNodeId()));
    }

    private Sinks.Many<ExecutionTrace> getOrCreateSink(String runId) {
        return sinksByRunId.computeIfAbsent(runId, k ->
                Sinks.many().multicast().directAllOrNothing()
        );
    }
    private void prepareForRun(String runId) {
        sinksByRunId.computeIfAbsent(runId, k ->
                Sinks.many().multicast().directAllOrNothing()
        );
    }
}