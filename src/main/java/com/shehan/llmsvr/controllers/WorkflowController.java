package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowEngine engine;
    private final WorkflowService workflowService;


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
