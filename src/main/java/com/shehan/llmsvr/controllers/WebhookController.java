package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebhookController {

    private final WorkflowEngine engine;
    private final WorkflowService workflowService;


    @PostMapping("/webhook/{workflowId}")
    public Mono<String> trigger(@PathVariable String workflowId, @RequestBody Map<String, Object> payload) {
        WorkflowDefinition workflow = workflowService.load(workflowId);
        MessageBatch batch = new MessageBatch(
                List.of(new WorkflowMessage(payload))
        );
        return engine.run(batch, workflow);
    }

}
