package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.FlowNode;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class WebhookController {

    private final WorkflowEngine engine;
    private final WorkflowService workflowService;


    @RequestMapping(value = "/{workflowId}/{nodeId}", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<Object>> handleHttp(@PathVariable String workflowId, @PathVariable String nodeId, ServerHttpRequest request, @RequestBody(required = false) Map<String, Object> body) {

        HttpMethod method = request.getMethod();
        Map<String, String> query = request.getQueryParams().toSingleValueMap();
        Map<String, String> headers = request.getHeaders().toSingleValueMap();

        WorkflowDefinition wf = workflowService.load(workflowId);
        FlowNode httpNode = wf.getNodes().stream().filter(n -> n.getId().equals(nodeId)).filter(n -> "trigger.http".equals(n.getType())).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid trigger.http node"));

        String expectedMethod = getInputProp(httpNode, "method", "POST");

        if (!expectedMethod.equalsIgnoreCase(method.name())) {
            return Mono.just(ResponseEntity.status(405).build());
        }
        MessageBatch batch = new MessageBatch(List.of(new WorkflowMessage(Map.of("provider", "http", "workflowId", workflowId, "nodeId", nodeId, "method", method.name(), "query", query, "headers", headers, "body", body))));
        return engine.runFromNode(batch, wf, nodeId).map(runId -> ResponseEntity.accepted().body(Map.of("status", "accepted", "workflowId", workflowId, "nodeId", nodeId, "runId", runId)));
    }

    private String getInputProp(FlowNode node, String name, String def) {

        if (node.getConfig() == null) return def;

        Object propsObj = node.getConfig();
        if (!(propsObj instanceof List<?> props)) return def;

        return props.stream()
                .filter(p -> p instanceof Map)
                .map(p -> (Map<String, Object>) p)
                .filter(p -> name.equals(p.get("name")))
                .map(p -> {
                    Object value = p.get("value");
                    if (value != null && !String.valueOf(value).isBlank()) {
                        return String.valueOf(value);
                    }
                    Object defaultValue = p.get("defaultValue");
                    return defaultValue != null
                            ? String.valueOf(defaultValue)
                            : def;
                })
                .findFirst()
                .orElse(def);
    }
}
