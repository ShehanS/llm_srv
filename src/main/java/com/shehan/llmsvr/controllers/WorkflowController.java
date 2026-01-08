package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.dtos.ResponseCode;
import com.shehan.llmsvr.dtos.ResponseMessage;
import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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


    @GetMapping("/open/{flowId}")
    public Mono<ResponseEntity<ResponseMessage>> save(@PathVariable String flowId) {
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
        return engine.getTrace(runId);
    }

    @GetMapping(value = "/runs/{runId}/trace/live",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveTrace(@PathVariable String runId) {
        return engine.liveTrace(runId);
    }


    @GetMapping(value = "/runs/{runId}/nodes/{nodeId}/trace/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExecutionTrace> liveNodeTrace(@PathVariable String runId, @PathVariable String nodeId) {
        return engine.liveNodeTrace(runId, nodeId);
    }

}
