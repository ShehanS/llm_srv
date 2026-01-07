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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        String runId = "test";
        FlowNode startNode = findStartNode(wf);

        ExecutionContext startCtx =
                new ExecutionContext(startNode, startMessages, runId, 0);
        Flux.just(startCtx)
                .expand(ctx -> executeNode(ctx, wf))
                .doOnComplete(() ->
                        log.info("✅ Workflow finished [runId={}]", runId))
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


    private Flux<ExecutionContext> executeNode(
            ExecutionContext ctx,
            WorkflowDefinition wf
    ) {
        FlowNode nodeDef = ctx.getNode();
        MessageBatch inputMessages = ctx.getMessages();

        Instant startedAt = Instant.now();

        log.info("Executing node [{}:{}]",
                nodeDef.getId(), nodeDef.getType());
        emitTrace(new ExecutionTrace(
                ctx.getRunId(),
                nodeDef.getId(),
                nodeDef.getType(),
                safeInput(inputMessages),
                null,
                nodeDef.getConfig(),
                "running",
                startedAt,
                null,
                null
        ));

        WorkflowNode node = registry.get(nodeDef.getType());
        if (node == null) {
            return Flux.error(new IllegalStateException(
                    "No WorkflowNode registered for type: " + nodeDef.getType()
            ));
        }

        return Mono.fromCallable(() ->
                        node.execute(inputMessages, nodeDef.getConfig())
                )
                .doOnSuccess(result -> {
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
                    return Flux.fromIterable(nextNodes)
                            .map(next ->
                                    new ExecutionContext(
                                            next,
                                            result.getMessages(),
                                            ctx.getRunId(),
                                            ctx.getAttempt()
                                    )
                            );
                });
    }

    private void emitTrace(ExecutionTrace trace) {
        traces.add(trace);
        traceSink.tryEmitNext(trace);
    }

    private Map<String, Object> safeInput(MessageBatch batch) {
        if (batch == null || batch.getItems().isEmpty()) {
            return null;
        }
        return batch.getItems().get(0).getData();
    }

    private List<FlowNode> findNextNodes(
            WorkflowDefinition wf,
            String sourceId,
            String output
    ) {
        return wf.getEdges().stream()
                .filter(e -> e.getSource().equals(sourceId))
                .filter(e -> e.getSourceHandle().equals(output))
                .map(e -> wf.getNodes().stream()
                        .filter(n -> n.getId().equals(e.getTarget()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private FlowNode findStartNode(WorkflowDefinition wf) {

        Set<String> targets = wf.getEdges().stream()
                .map(FlowEdge::getTarget)
                .collect(Collectors.toSet());

        return wf.getNodes().stream()
                .filter(n -> !targets.contains(n.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No start node found"));
    }
}
