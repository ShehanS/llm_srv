package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.mcpTools.ApprovalTool;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowEngine engine;
    private final WorkflowService workflowService;
    private final ApprovalTool approvalTool;
    private final WebClient webClient;

    @PostMapping("/save")
    public Mono<ResponseEntity<ResponseMessage>> save(@RequestBody Workflow flow) {
        return workflowService.save(flow)
                .map(res ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Workflow saved successfully",
                                        res,
                                        null)
                        )
                )
                .onErrorResume(ex ->
                        Mono.just(
                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(
                                                new ResponseMessage(
                                                        ResponseCode.ERROR.getCode(),
                                                        null,
                                                        ex.getMessage(),
                                                        "Workflow save failed"
                                                )
                                        )
                        )
                );
    }

    @PostMapping("/approval/{runId}/{outputHandle}")
    public Mono<ResponseEntity<ResponseMessage>> approval(@PathVariable String runId, @RequestBody MessageBatch humanInput, @PathVariable String outputHandle) {
        return engine.resume(runId, humanInput, outputHandle)
                .map(res ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Workflow continue successfully",
                                        res,
                                        null)
                        )
                )
                .onErrorResume(ex ->
                        Mono.just(
                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(
                                                new ResponseMessage(
                                                        ResponseCode.ERROR.getCode(),
                                                        null,
                                                        ex.getMessage(),
                                                        "Workflow save failed"
                                                )
                                        )
                        )
                );
    }

    @GetMapping("/open/{flowId}")
    public Mono<ResponseEntity<ResponseMessage>> open(@PathVariable String flowId) {
        return workflowService.open(flowId)
                .map(res ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Workflow open successfully",
                                        res,
                                        null)
                        )
                )
                .onErrorResume(ex ->
                        Mono.just(
                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(
                                                new ResponseMessage(
                                                        ResponseCode.ERROR.getCode(),
                                                        null,
                                                        ex.getMessage(),
                                                        "Workflow open failed"
                                                )
                                        )
                        )
                );
    }

    @GetMapping("/open/all")
    public Mono<ResponseEntity<ResponseMessage>> getAll() {
        return workflowService.getAll()
                .collectList()
                .map(res ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Workflows load successfully",
                                        res,
                                        null
                                )
                        )
                )
                .onErrorResume(ex ->
                        Mono.just(
                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(
                                                new ResponseMessage(
                                                        ResponseCode.ERROR.getCode(),
                                                        null,
                                                        ex.getMessage(),
                                                        "Workflows load failed"
                                                )
                                        )
                        )
                );
    }

    @GetMapping("/runs/{runId}/trace")
    public Flux<ExecutionTrace> getTrace(@PathVariable String runId) {
        log.info("Getting historical traces for runId: {}", runId);
        return engine.getTrace(runId);
    }

    @GetMapping(value = "/runs/{runId}/trace/live",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveTrace(@PathVariable String runId) {
        log.info("SSE connection established for runId: {}", runId);
        return engine.liveTrace(runId)
                .doOnSubscribe(s -> log.info("SSE subscribed for runId: {}", runId))
                .doOnComplete(() -> log.info("SSE completed for runId: {}", runId))
                .doOnCancel(() -> log.info("SSE cancelled for runId: {}", runId));
    }

    @GetMapping(value = "/runs/{runId}/nodes/{nodeId}/trace/live",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveNodeTrace(
            @PathVariable String runId,
            @PathVariable String nodeId) {
        log.info("SSE node trace for runId: {}, nodeId: {}", runId, nodeId);
        return engine.liveNodeTrace(runId, nodeId)
                .doOnSubscribe(s -> log.info("SSE node trace subscribed [runId={}, nodeId={}]",
                        runId, nodeId));
    }

    @GetMapping("/runs/{runId}/status")
    public Mono<ResponseEntity<ResponseMessage>> getWorkflowStatus(@PathVariable String runId) {
        return Mono.just(
                ResponseEntity.ok(
                        new ResponseMessage(
                                ResponseCode.SUCCESS.getCode(),
                                "Workflow status retrieved",
                                new WorkflowStatus(runId),
                                null
                        )
                )
        );
    }

    @DeleteMapping("/runs/{runId}/reset")
    public Mono<ResponseEntity<ResponseMessage>> resetRunId(@PathVariable String runId) {
        log.info("Resetting runId: {}", runId);
        return Mono.just(
                ResponseEntity.ok(
                        new ResponseMessage(
                                ResponseCode.SUCCESS.getCode(),
                                "RunId reset successfully",
                                Map.of("runId", runId),
                                null
                        )
                )
        );
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<ResponseMessage>> health() {
        log.info("Health check called");
        return Mono.just(
                ResponseEntity.ok(
                        new ResponseMessage(
                                ResponseCode.SUCCESS.getCode(),
                                "Service is running",
                                Map.of("status", "UP", "timestamp", System.currentTimeMillis()),
                                null
                        )
                )
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, ApprovalTool.PendingApproval>> getPendingApprovals() {
        return ResponseEntity.ok(approvalTool.getPendingApprovals());
    }

    @PostMapping("/session/{sessionId}/decide")
    public Mono<ResponseEntity<Map<String, Object>>> submitDecisionBySession(
            @PathVariable String sessionId,
            @RequestBody ApprovalDecisionRequest request
    ) {
        log.info("Submitting decision for session: {} - action: {}", sessionId, request.getAction());

        Map<String, ApprovalTool.PendingApproval> pending = approvalTool.getPendingApprovals();
        ApprovalTool.PendingApproval targetApproval = null;
        String targetRequestId = null;

        for (Map.Entry<String, ApprovalTool.PendingApproval> entry : pending.entrySet()) {
            if (entry.getKey().startsWith(sessionId + "_") && "pending".equals(entry.getValue().getStatus())) {
                targetApproval = entry.getValue();
                targetRequestId = entry.getKey();
                log.info("Found pending approval: {}", targetRequestId);
                break;
            }
        }

        if (targetApproval == null) {
            log.warn("No pending approval found for session: {}", sessionId);
            return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No pending approval found for session: " + sessionId))
            );
        }

        Map<String, Object> approvalResult = approvalTool.submitApprovalDecision(
                targetRequestId,
                request.getAction(),
                request.getFeedback()
        );

        log.info("Approval marked as {} for requestId: {}", request.getAction(), targetRequestId);

        String finalTargetRequestId = targetRequestId;

        Map<String, String> resumePayload = Map.of(
                "action", request.getAction(),
                "feedback", request.getFeedback() != null ? request.getFeedback() : ""
        );

        return webClient.post()
                .uri("/api/v1/chat/resume/" + sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(resumePayload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(nodeResponse -> {
                    log.info("Node.js resume completed successfully");

                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "success");
                    result.put("sessionId", sessionId);
                    result.put("requestId", finalTargetRequestId);
                    result.put("action", request.getAction());
                    result.put("workflowResumed", true);
                    result.put("response", nodeResponse);

                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("Failed to resume Node.js workflow: {}", error.getMessage(), error);

                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "error");
                    result.put("sessionId", sessionId);
                    result.put("requestId", finalTargetRequestId);
                    result.put("approvalRecorded", true);
                    result.put("workflowResumed", false);
                    result.put("error", error.getMessage());

                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result));
                });
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> createApprovalRequest(
            @RequestBody ApprovalRequestDTO request
    ) {
        log.info("Creating approval request: {} for session: {}", request.getRequestId(), request.getSessionId());
        Map<String, Object> result = approvalTool.requestApproval(
                request.getRequestId(),
                request.getToolName(),
                request.getToolArgs(),
                request.getDescription()
        );
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> clearApproval(@PathVariable String requestId) {
        log.info("Clearing approval: {}", requestId);
        approvalTool.clearApproval(requestId);
        return ResponseEntity.noContent().build();
    }

    public static class ApprovalRequestDTO {
        private String requestId;
        private String toolName;
        private String toolArgs;
        private String description;
        private String sessionId;

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public String getToolArgs() { return toolArgs; }
        public void setToolArgs(String toolArgs) { this.toolArgs = toolArgs; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    public static class ApprovalDecisionRequest {
        private String action;
        private String feedback;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    private record WorkflowStatus(String runId) {}
}
