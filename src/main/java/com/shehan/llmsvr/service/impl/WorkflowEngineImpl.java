package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.nodes.WorkflowNode;
import com.shehan.llmsvr.service.NodeRegistry;
import com.shehan.llmsvr.service.WorkflowEngine;
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
    private final Map<String, List<ExecutionTrace>> tracesByRunId = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<ExecutionTrace>> sinksByRunId = new ConcurrentHashMap<>();
    private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();

    public WorkflowEngineImpl(NodeRegistry registry) {
        this.registry = registry;
    }

    private Sinks.Many<ExecutionTrace> getOrCreateSink(String runId) {
        return sinksByRunId.computeIfAbsent(runId, k -> {
            log.info("Creating new Multicast sink for runId: {}", runId);
            return Sinks.many().multicast().directBestEffort();
        });
    }

    @Override
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf, String runId) {
        prepareForRun(runId);

        FlowNode startNode = findStartNode(wf);
        executeWorkflow(new ExecutionContext(startNode, startMessages, runId, 0), wf);

        return Mono.just(runId);
    }

    @Override
    public Mono<String> runFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId) {
        String runId = UUID.randomUUID().toString();
        prepareForRun(runId);

        FlowNode start = wf.getNodes().stream()
                .filter(n -> n.getId().equals(startNodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Start node not found: " + startNodeId));

        executeWorkflow(new ExecutionContext(start, batch, runId, 0), wf);

        return Mono.just(runId);
    }

    @Override
    public Flux<ExecutionTrace> getTrace(String runId) {
        List<ExecutionTrace> traces = tracesByRunId.getOrDefault(runId, Collections.emptyList());
        return Flux.fromIterable(new ArrayList<>(traces));
    }

    @Override
    public Flux<ExecutionTrace> liveTrace(String runId) {
        return getOrCreateSink(runId).asFlux()
                .doOnSubscribe(s -> log.info("WS Subscriber joined liveTrace: {}", runId));
    }

    @Override
    public Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId) {
        return getOrCreateSink(runId).asFlux()
                .filter(t -> t.getNodeId().equals(nodeId))
                .doOnSubscribe(s -> log.info("WS Subscriber joined liveNodeTrace: {}/{}", runId, nodeId));
    }

    private void prepareForRun(String runId) {
        tracesByRunId.remove(runId);
        activeRuns.add(runId);
        getOrCreateSink(runId);
    }

    private void executeWorkflow(ExecutionContext ctx, WorkflowDefinition wf) {
        Flux.just(ctx)
                .expand(c -> executeNode(c, wf))
                .doOnComplete(() -> {
                    log.info("Workflow finished [runId={}]", ctx.getRunId());
                    activeRuns.remove(ctx.getRunId());
                })
                .doOnError(error -> {
                    log.error("Workflow failed [runId={}]: {}", ctx.getRunId(), error.getMessage());
                    activeRuns.remove(ctx.getRunId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Flux<ExecutionContext> executeNode(ExecutionContext ctx, WorkflowDefinition wf) {
        FlowNode nodeDef = ctx.getNode();
        MessageBatch inputMessages = ctx.getMessages();
        Instant startedAt = Instant.now();

        emitTrace(new ExecutionTrace(
                ctx.getRunId(), nodeDef.getId(), nodeDef.getType(),
                inputMessages, null, nodeDef.getConfig(),
                ExecutionTrace.Status.RUNNING, startedAt, null, null
        ));

        WorkflowNode node = registry.get(nodeDef.getType());
        if (node == null) return Flux.error(new IllegalStateException("Node type not found: " + nodeDef.getType()));

        return Mono.fromCallable(() -> node.execute(inputMessages, nodeDef.getConfig()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> {
                    emitTrace(ExecutionTrace.builder()
                            .runId(ctx.getRunId())
                            .nodeId(nodeDef.getId())
                            .nodeType(nodeDef.getType())
                            .input(safeInput(inputMessages))
                            .output(safeInput(result.getMessages()))
                            .status(ExecutionTrace.Status.COMPLETE)
                            .startedAt(startedAt)
                            .completedAt(Instant.now())
                            .build());
                })
                .flatMapMany(result -> {
                    List<FlowNode> nextNodes = findNextNodes(wf, nodeDef.getId(), result.getOutput());
                    return Flux.fromIterable(nextNodes)
                            .map(next -> new ExecutionContext(next, result.getMessages(), ctx.getRunId(), ctx.getAttempt()));
                });
    }

    private void emitTrace(ExecutionTrace trace) {
        tracesByRunId.computeIfAbsent(trace.getRunId(), k -> new CopyOnWriteArrayList<>()).add(trace);
        Sinks.Many<ExecutionTrace> sink = getOrCreateSink(trace.getRunId());
        sink.emitNext(trace, Sinks.EmitFailureHandler.FAIL_FAST);
    }


    private Map<String, Object> safeInput(MessageBatch batch) {
        if (batch == null || batch.getItems() == null || batch.getItems().isEmpty()) return null;
        return batch.getItems().get(0).getData();
    }

    private List<FlowNode> findNextNodes(WorkflowDefinition wf, String sourceId, String output) {
        return wf.getEdges().stream()
                .filter(e -> sourceId.equals(e.getSource()) && output.equals(e.getSourceHandle()))
                .map(e -> wf.getNodes().stream().filter(n -> n.getId().equals(e.getTarget())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private FlowNode findStartNode(WorkflowDefinition wf) {
        Set<String> targets = new HashSet<>();
        wf.getEdges().forEach(e -> targets.add(e.getTarget()));
        return wf.getNodes().stream()
                .filter(n -> !targets.contains(n.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No start node found"));
    }
}
