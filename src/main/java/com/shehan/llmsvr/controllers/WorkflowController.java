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

import java.time.Instant;
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
                .map(res -> ResponseEntity.ok(ResponseMessage.builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .message("Workflow saved successfully")
                        .data(res)
                        .build()));
    }

    @PostMapping("/approval/{runId}/{outputHandle}")
    public Mono<ResponseEntity<ResponseMessage>> approval(@PathVariable String runId,
                                                          @RequestBody MessageBatch humanInput,
                                                          @PathVariable String outputHandle) {
        return engine.resume(runId, humanInput, outputHandle)
                .map(res -> ResponseEntity.ok(ResponseMessage.builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .message("Workflow continue successfully")
                        .data(res)
                        .build()));
    }

    @GetMapping("/open/{flowId}")
    public Mono<ResponseEntity<ResponseMessage>> open(@PathVariable String flowId) {
        return workflowService.open(flowId)
                .map(res -> ResponseEntity.ok(ResponseMessage.builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .message("Workflow open successfully")
                        .data(res)
                        .build()));
    }

    @GetMapping("/open/all")
    public Mono<ResponseEntity<ResponseMessage>> getAll() {
        return workflowService.getAll()
                .collectList()
                .map(res -> ResponseEntity.ok(ResponseMessage.builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .message("Workflows load successfully")
                        .data(res)
                        .build()));
    }

    @GetMapping("/runs/{runId}/trace")
    public Flux<ExecutionTrace> getTrace(@PathVariable String runId) {
        log.info("Getting historical traces for runId: {}", runId);
        return engine.getTrace(runId);
    }

    @GetMapping(value = "/runs/{runId}/trace/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveTrace(@PathVariable String runId) {
        return engine.liveTrace(runId);
    }

    @GetMapping(value = "/runs/{runId}/nodes/{nodeId}/trace/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveNodeTrace(@PathVariable String runId, @PathVariable String nodeId) {
        return engine.liveNodeTrace(runId, nodeId);
    }

    @GetMapping("/runs/{runId}/status")
    public Mono<ResponseEntity<ResponseMessage>> getWorkflowStatus(@PathVariable String runId) {
        return Mono.just(ResponseEntity.ok(ResponseMessage.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message("Workflow status retrieved")
                .data(new WorkflowStatus(runId))
                .build()));
    }

    @DeleteMapping("/runs/{runId}/reset")
    public Mono<ResponseEntity<ResponseMessage>> resetRunId(@PathVariable String runId) {
        return Mono.just(ResponseEntity.ok(ResponseMessage.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message("RunId reset successfully")
                .data(Map.of("runId", runId))
                .build()));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<ResponseMessage>> health() {
        return Mono.just(ResponseEntity.ok(ResponseMessage.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message("Service is running")
                .data(Map.of("status", "UP", "timestamp", Instant.now().toEpochMilli()))
                .build()));
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, PendingApproval>> getPendingApprovals() {
        return ResponseEntity.ok(approvalTool.getPendingApprovals());
    }

    @PostMapping("/session/{sessionId}/decide")
    public Mono<ResponseEntity<Map<String, Object>>> submitDecisionBySession(
            @PathVariable String sessionId,
            @RequestBody ApprovalDecisionRequest request) {

        Map<String, PendingApproval> pending = approvalTool.getPendingApprovals();
        String targetRequestId = pending.entrySet().stream()
                .filter(e -> e.getKey().startsWith(sessionId + "_") && "pending".equals(e.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (targetRequestId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No pending approval found")));
        }

        approvalTool.submitApprovalDecision(targetRequestId, request.getAction(), request.getFeedback());

        return webClient.post()
                .uri("/api/v1/conversation/resume/" + sessionId)
                .bodyValue(Map.of("action", request.getAction(), "feedback", request.getFeedback() != null ? request.getFeedback() : ""))
                .retrieve()
                .bodyToMono(Map.class)
                .map(nodeResponse -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "workflowResumed", true,
                        "response", nodeResponse)))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()))));
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> createApprovalRequest(@RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(approvalTool.requestApproval(
                request.getRequestId(), request.getToolName(), request.getToolArgs(), request.getDescription()));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> clearApproval(@PathVariable String requestId) {
        approvalTool.clearApproval(requestId);
        return ResponseEntity.noContent().build();
    }

    private record WorkflowStatus(String runId) {}
}
