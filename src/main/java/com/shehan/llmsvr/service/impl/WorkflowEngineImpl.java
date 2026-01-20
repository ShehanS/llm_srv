package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.nodes.WorkflowNode;
import com.shehan.llmsvr.service.NodeRegistry;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowStateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class WorkflowEngineImpl implements WorkflowEngine {

    private final NodeRegistry registry;
    private final WorkflowStateManager stateManager;
    private final Map<String, List<ExecutionTrace>> tracesByRunId = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<ExecutionTrace>> sinksByRunId = new ConcurrentHashMap<>();
    private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();

    public WorkflowEngineImpl(NodeRegistry registry, WorkflowStateManager stateManager) {
        this.registry = registry;
        this.stateManager = stateManager;
    }

    @Override
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf, String runId) {
        prepareForRun(runId);
        FlowNode startNode = findStartNode(wf);
        executeWorkflow(new ExecutionContext(startNode, startMessages, runId, 0), wf);
        return Mono.just(runId);
    }

    @Override
    public Mono<String> resume(String runId, MessageBatch humanInput, String outputHandle) {
        WorkflowStateManager.SuspendedState suspended = stateManager.getAndRemove(runId);
        if (suspended == null) {
            return Mono.error(new IllegalStateException("No suspended workflow found for runId: " + runId));
        }
        FlowNode suspendedNode = suspended.getCtx().getNode();
        humanInput.getItems().forEach(item -> {
            if (!item.getData().containsKey("action")) {
                item.getData().put("action", outputHandle);
            }
        });
        log.info("Resuming runId: {} by re-executing HumanApprovalNode: {}", runId, suspendedNode.getId());
        executeWorkflow(new ExecutionContext(suspendedNode, humanInput, runId, 0), suspended.getWf());
        return Mono.just(runId);
    }

    private void executeWorkflow(ExecutionContext ctx, WorkflowDefinition wf) {
        Flux.just(ctx)
                .expand(c -> executeNode(c, wf))
                .doOnComplete(() -> {
                    log.info("Workflow execution finished for runId: {}", ctx.getRunId());
                    activeRuns.remove(ctx.getRunId());
                })
                .doOnError(error -> {
                    log.error("Workflow failed for runId {}: {}", ctx.getRunId(), error.getMessage());
                    activeRuns.remove(ctx.getRunId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Flux<ExecutionContext> executeNode(ExecutionContext ctx, WorkflowDefinition wf) {
        FlowNode nodeDef = ctx.getNode();
        MessageBatch inputMessages = ctx.getMessages();
        Instant startedAt = Instant.now();
        emitTrace(createTrace(ctx, ExecutionTrace.Status.RUNNING, startedAt, inputMessages, null, null));

        WorkflowNode node = registry.get(nodeDef.getType());
        return Mono.fromCallable(() -> node.execute(inputMessages, nodeDef.getConfig()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(result -> {
                    if (result.getStatus() == NodeResult.Status.WAITING) {
                        emitTrace(createTrace(ctx, ExecutionTrace.Status.WAITING, startedAt, inputMessages, null, result.getWaitPayload()));
                        stateManager.save(ctx.getRunId(), wf, ctx);
                        return Flux.empty();
                    }
                    if (result.getStatus() == NodeResult.Status.ERROR) {
                        emitTrace(createTrace(ctx, ExecutionTrace.Status.FAILED, startedAt, inputMessages, result.getMessages(), "Node execution failed"));
                        return Flux.error(new RuntimeException("Node returned error status"));
                    }


                    emitTrace(createTrace(ctx, ExecutionTrace.Status.COMPLETE, startedAt, inputMessages, result.getMessages(), null));
                    List<FlowNode> nextNodes = findNextNodes(wf, nodeDef.getId(), result.getOutput());
                    return Flux.fromIterable(nextNodes)
                            .map(next -> new ExecutionContext(next, result.getMessages(), ctx.getRunId(), ctx.getAttempt()));
                })
                .onErrorResume(e -> {
                    emitTrace(createTrace(ctx, ExecutionTrace.Status.FAILED, startedAt, inputMessages, null, e.getMessage()));
                    return Flux.error(e);
                });
    }

    private List<FlowNode> findNextNodes(WorkflowDefinition wf, String sourceId, String output) {
        return wf.getEdges().stream()
                .filter(e -> sourceId.equals(e.getSource()) && output.equals(e.getSourceHandle()))
                .map(e -> wf.getNodes().stream().filter(n -> n.getId().equals(e.getTarget())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void emitTrace(ExecutionTrace trace) {
        tracesByRunId.computeIfAbsent(trace.getRunId(), k -> new CopyOnWriteArrayList<>()).add(trace);
        getOrCreateSink(trace.getRunId()).emitNext(trace, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private Sinks.Many<ExecutionTrace> getOrCreateSink(String runId) {
        return sinksByRunId.computeIfAbsent(runId, k -> Sinks.many().multicast().directBestEffort());
    }

    private void prepareForRun(String runId) {
        tracesByRunId.remove(runId);
        activeRuns.add(runId);
        getOrCreateSink(runId);
    }

    private FlowNode findStartNode(WorkflowDefinition wf) {
        Set<String> targets = new HashSet<>();
        wf.getEdges().forEach(e -> targets.add(e.getTarget()));
        return wf.getNodes().stream()
                .filter(n -> !targets.contains(n.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Start node not found"));
    }

    private ExecutionTrace createTrace(ExecutionContext ctx, ExecutionTrace.Status status, Instant start, MessageBatch input, MessageBatch output, Object metadata) {
        return ExecutionTrace.builder()
                .runId(ctx.getRunId())
                .nodeId(ctx.getNode().getId())
                .nodeType(ctx.getNode().getType())
                .config(ctx.getNode().getConfig())
                .input(input)
                .output(output)
                .status(status)
                .startedAt(start)
                .completedAt(status == ExecutionTrace.Status.RUNNING ? null : Instant.now())
                .metadata(metadata)
                .build();
    }

    @Override
    public Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String flowId) {
        prepareForRun(flowId);
        FlowNode start = wf.getNodes().stream().filter(n -> n.getId().equals(startNodeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node not found"));
        executeWorkflow(new ExecutionContext(start, batch, flowId, 0), wf);
        return Mono.just(flowId);
    }

    @Override
    public Flux<ExecutionTrace> getTrace(String runId) {
        return Flux.fromIterable(tracesByRunId.getOrDefault(runId, Collections.emptyList()));
    }

    @Override
    public Flux<ExecutionTrace> liveTrace(String runId) {
        return getOrCreateSink(runId).asFlux();
    }

    @Override
    public Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId) {
        return liveTrace(runId).filter(t -> t.getNodeId().equals(nodeId));
    }
}
