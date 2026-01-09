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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowEngineImpl implements WorkflowEngine {

    private final NodeRegistry registry;

    private final List<ExecutionTrace> traces = new CopyOnWriteArrayList<>();
    private final Sinks.Many<ExecutionTrace> traceSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public WorkflowEngineImpl(NodeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<String> run(MessageBatch startMessages, WorkflowDefinition wf) {
        String runId = UUID.randomUUID().toString();
        FlowNode startNode = findStartNode(wf);
        ExecutionContext startCtx =
                new ExecutionContext(startNode, startMessages, runId, 0);
        Flux.just(startCtx)
                .expand(ctx -> executeNode(ctx, wf))
                .doOnComplete(() ->
                        log.info("Workflow finished [runId={}]", runId))
                .doOnError(error ->
                        log.error("Workflow failed [runId={}]: {}", runId, error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        return Mono.just(runId);
    }

    @Override
    public Flux<ExecutionTrace> getTrace(String runId) {
        return Flux.fromIterable(traces)
                .filter(t -> t.getRunId().equals(runId));
    }

    @Override
    public Flux<ExecutionTrace> liveTrace(String runId) {
        return traceSink.asFlux()
                .filter(t -> t.getRunId().equals(runId));
    }

    @Override
    public Flux<ExecutionTrace> liveNodeTrace(String runId, String nodeId) {
        return traceSink.asFlux()
                .filter(t -> t.getRunId().equals(runId))
                .filter(t -> t.getNodeId().equals(nodeId));
    }

    @Override
    public Mono<String> runFromNode(
            MessageBatch batch,
            WorkflowDefinition wf,
            String startNodeId
    ) {
        String runId = UUID.randomUUID().toString();
        FlowNode start = wf.getNodes().stream()
                .filter(n -> n.getId().equals(startNodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Start node not found: " + startNodeId));
        ExecutionContext ctx =
                new ExecutionContext(start, batch, runId, 0);
        Flux.just(ctx)
                .expand(c -> executeNode(c, wf))
                .doOnComplete(() ->
                        log.info("Workflow finished from node [runId={}, nodeId={}]",
                                runId, startNodeId))
                .doOnError(error ->
                        log.error("Workflow failed from node [runId={}, nodeId={}]: {}",
                                runId, startNodeId, error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        return Mono.just(runId);
    }

    private Flux<ExecutionContext> executeNode(ExecutionContext ctx, WorkflowDefinition wf) {
        FlowNode nodeDef = ctx.getNode();
        MessageBatch inputMessages = ctx.getMessages();
        Instant startedAt = Instant.now();
        log.info("Executing node [id={}, type={}]", nodeDef.getId(), nodeDef.getType());
        emitTrace(new ExecutionTrace(
                ctx.getRunId(),
                nodeDef.getId(),
                nodeDef.getType(),
                inputMessages,
                null,
                nodeDef.getConfig(),
                "running",
                startedAt,
                null,
                null
        ));
        WorkflowNode node = registry.get(nodeDef.getType());
        if (node == null) {
            String errorMsg = "No WorkflowNode registered for type: " + nodeDef.getType();
            log.error("Error {}", errorMsg);
            return Flux.error(new IllegalStateException(errorMsg));
        }

        return Mono.fromCallable(() ->
                        node.execute(inputMessages, nodeDef.getConfig())
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> {
                    log.info("Node completed [id={}, output={}]",
                            nodeDef.getId(), result.getOutput());

                    emitTrace(new ExecutionTrace(
                            ctx.getRunId(),
                            nodeDef.getId(),
                            nodeDef.getType(),
                            safeInput(inputMessages),
                            safeInput(result.getMessages()),
                            nodeDef.getConfig(),
                            result.getOutput(),
                            startedAt,
                            Instant.now(),
                            null
                    ));
                })
                .doOnError(error -> {
                    log.error("Node failed [id={}, type={}]: {}",
                            nodeDef.getId(), nodeDef.getType(), error.getMessage());

                    emitTrace(new ExecutionTrace(
                            ctx.getRunId(),
                            nodeDef.getId(),
                            nodeDef.getType(),
                            safeInput(inputMessages),
                            null,
                            nodeDef.getConfig(),
                            "error",
                            startedAt,
                            Instant.now(),
                            error.getMessage()
                    ));
                })
                .flatMapMany(result -> {
                    List<FlowNode> nextNodes =
                            findNextNodes(wf, nodeDef.getId(), result.getOutput());

                    if (nextNodes.isEmpty()) {
                        log.info("No more nodes to execute [nodeId={}]", nodeDef.getId());
                    } else {
                        log.info("Moving to {} next node(s)", nextNodes.size());
                    }

                    return Flux.fromIterable(nextNodes)
                            .map(next ->
                                    new ExecutionContext(
                                            next,
                                            result.getMessages(),
                                            ctx.getRunId(),
                                            ctx.getAttempt()
                                    )
                            );
                })
                .onErrorResume(error -> {
                    log.error("Stopping workflow due to error in node [id={}]",
                            nodeDef.getId());
                    return Flux.empty();
                });
    }

    private void emitTrace(ExecutionTrace trace) {
        traces.add(trace);
        Sinks.EmitResult result = traceSink.tryEmitNext(trace);
        if (result.isFailure()) {
            log.warn("Failed to emit trace: {}", result);
        }
    }

    private Map<String, Object> safeInput(MessageBatch batch) {
        if (batch == null || batch.getItems() == null || batch.getItems().isEmpty()) {
            return null;
        }

        WorkflowMessage firstItem = batch.getItems().get(0);
        return firstItem != null ? firstItem.getData() : null;
    }

    private List<FlowNode> findNextNodes(WorkflowDefinition wf, String sourceId, String output) {
        if (wf == null || wf.getEdges() == null || wf.getNodes() == null) {
            log.warn("Invalid workflow definition");
            return Collections.emptyList();
        }

        if (output == null) {
            log.warn("Node output is null, cannot find next nodes [sourceId={}]", sourceId);
            return Collections.emptyList();
        }

        return wf.getEdges().stream()
                .filter(e -> e != null)
                .filter(e -> sourceId.equals(e.getSource()))
                .filter(e -> output.equals(e.getSourceHandle()))
                .map(e -> wf.getNodes().stream()
                        .filter(n -> n != null)
                        .filter(n -> e.getTarget().equals(n.getId()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private FlowNode findStartNode(WorkflowDefinition wf) {
        if (wf == null || wf.getNodes() == null || wf.getEdges() == null) {
            throw new IllegalStateException("Invalid workflow definition");
        }

        if (wf.getNodes().isEmpty()) {
            throw new IllegalStateException("Workflow has no nodes");
        }
        Set<String> targets = wf.getEdges().stream()
                .filter(e -> e != null)
                .map(FlowEdge::getTarget)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return wf.getNodes().stream()
                .filter(n -> n != null)
                .filter(n -> !targets.contains(n.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No start node found (all nodes have incoming edges)"));
    }
}
