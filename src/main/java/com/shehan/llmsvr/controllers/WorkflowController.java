package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowEngine engine;
    private final WorkflowService workflowService;


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
    private record WorkflowStatus(String runId) {}
}
