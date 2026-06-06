package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.service.WorkflowOrchestrator;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.ActivityStub;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
public class WorkflowOrchestratorImpl implements
        WorkflowOrchestrator.StandardEngine,
        WorkflowOrchestrator.NodeEngine,
        WorkflowOrchestrator.ParallelEngine {

    private final ActivityOptions options = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .build();

    private final ActivityStub untypedActivityStub = Workflow.newUntypedActivityStub(options);
    private final WorkflowOrchestrator.TraceActivity traceActivityStub = Workflow.newActivityStub(WorkflowOrchestrator.TraceActivity.class, options);

    private MessageBatch humanInputPayload = null;
    private String targetedOutputHandle = null;

    @Override
    public String runTemporalWorkflow(MessageBatch startMessages, WorkflowDefinition wf, String runId) {
        log.info("Starting standard workflow run sequence for runId: {}", runId);
        FlowNode startNode = findStartNode(wf);
        executeGraph(startNode, startMessages, wf, runId);
        return runId;
    }

    @Override
    public String runTemporalFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String flowId) {
        log.info("Starting workflow run context from specific node: {} for flowId: {}", startNodeId, flowId);
        if (wf.getNodes() != null) {
            for (FlowNode node : wf.getNodes()) {
                if (node.getId().equals(startNodeId)) {
                    executeGraph(node, batch, wf, flowId);
                    break;
                }
            }
        }
        return flowId;
    }

    @Override
    public String runTemporalMultipleNodes(MessageBatch batch, WorkflowDefinition wf, List<String> nodeIds, String flowId) {
        log.info("Starting parallel multi-node parallel workflow run for flowId: {}", flowId);
        List<Promise<Void>> parallelTasks = new ArrayList<>();
        if (wf.getNodes() != null && nodeIds != null) {
            for (String id : nodeIds) {
                for (FlowNode node : wf.getNodes()) {
                    if (node.getId().equals(id)) {
                        Promise<Void> task = Async.procedure(() -> executeGraph(node, batch, wf, flowId));
                        parallelTasks.add(task);
                        break;
                    }
                }
            }
        }
        Promise.allOf(parallelTasks).get();
        return flowId;
    }

    @Override
    public void resumeSignal(MessageBatch humanInput, String outputHandle) {
        log.info("Received workflow resume validation event signal targeting handle: {}", outputHandle);
        this.humanInputPayload = humanInput;
        this.targetedOutputHandle = outputHandle;
    }

    private void executeGraph(FlowNode startNode, MessageBatch initialMessages, WorkflowDefinition wf, String runId) {
        Queue<ExecutionStep> queue = new LinkedList<>();
        queue.add(new ExecutionStep(startNode, initialMessages));

        while (!queue.isEmpty()) {
            ExecutionStep currentStep = queue.poll();
            FlowNode nodeDef = currentStep.getNode();
            MessageBatch inputMessages = currentStep.getMessages();

            Instant startTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
            emitTraceActivity(runId, nodeDef, inputMessages, null, ExecutionTrace.Status.RUNNING, startTime, null, null);

            NodeResult result;
            try {
                log.info("Invoking Untyped dynamic router for node definition type: {}", nodeDef.getType());
                result = untypedActivityStub.execute(
                        nodeDef.getType(),
                        NodeResult.class,
                        inputMessages,
                        nodeDef.getConfig()
                );
            } catch (Exception e) {
                Instant errorTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
                emitTraceActivity(runId, nodeDef, inputMessages, null, ExecutionTrace.Status.FAILED, startTime, errorTime, e.getMessage());
                throw new RuntimeException("Activity execution failed at node: " + nodeDef.getId(), e);
            }

            if (result.getStatus() == NodeResult.Status.WAITING) {
                log.info("Workflow paused at execution node: {}. Waiting for verification input signal...", nodeDef.getId());

                Instant waitingTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
                emitTraceActivity(runId, nodeDef, inputMessages, result.getMessages(), ExecutionTrace.Status.WAITING, startTime, waitingTime, null);

                Workflow.await(() -> humanInputPayload != null);

                MessageBatch resumedInput = this.humanInputPayload;
                String handle = this.targetedOutputHandle;

                this.humanInputPayload = null;
                this.targetedOutputHandle = null;

                if (resumedInput != null && resumedInput.getItems() != null) {
                    resumedInput.getItems().forEach(item -> {
                        if (item.getData() != null && !item.getData().containsKey("action")) {
                            item.getData().put("action", handle);
                        }
                    });
                }

                queue.add(new ExecutionStep(nodeDef, resumedInput));
                continue;
            }

            if (result.getStatus() == NodeResult.Status.ERROR) {
                Instant errorTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
                emitTraceActivity(runId, nodeDef, inputMessages, result.getMessages(), ExecutionTrace.Status.ERROR, startTime, errorTime, "Workflow node execution error returned from worker backend implementation");
                throw new RuntimeException("Workflow execution failed at node: " + nodeDef.getId());
            }

            Instant completeTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
            emitTraceActivity(runId, nodeDef, inputMessages, result.getMessages(), ExecutionTrace.Status.COMPLETE, startTime, completeTime, null);

            MessageBatch outputMessages = result.getMessages() != null ? result.getMessages() : inputMessages;
            String outputHandle = result.getOutput() != null ? result.getOutput() : "";

            List<FlowNode> nextNodes = findNextNodes(wf, nodeDef.getId(), outputHandle);
            for (FlowNode next : nextNodes) {
                queue.add(new ExecutionStep(next, outputMessages));
            }
        }
    }

    private void emitTraceActivity(String runId, FlowNode nodeDef, Object input, Object output, ExecutionTrace.Status status, Instant startedAt, Instant completedAt, String error) {
        try {
            ExecutionTrace trace = ExecutionTrace.builder()
                    .runId(runId)
                    .nodeId(nodeDef.getId())
                    .nodeType(nodeDef.getType())
                    .config(nodeDef.getConfig())
                    .input(input)
                    .output(output)
                    .status(status)
                    .startedAt(startedAt)
                    .completedAt(completedAt)
                    .error(error)
                    .build();

            traceActivityStub.emitTrace(trace);
        } catch (Exception e) {
            log.warn("Tracing exception: {}", e.getMessage());
        }
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
        if (wf.getEdges() != null) {
            wf.getEdges().forEach(e -> targets.add(e.getTarget()));
        }
        if (wf.getNodes() != null) {
            for (FlowNode n : wf.getNodes()) {
                if (!targets.contains(n.getId())) {
                    return n;
                }
            }
        }
        throw new IllegalStateException("Start node not found");
    }
}