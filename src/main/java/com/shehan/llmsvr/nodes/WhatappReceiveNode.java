package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WhatappReceiveNode implements WorkflowNode {

    private final WorkflowService workflowService;
    private final WorkflowEngine engine;

    public WhatappReceiveNode(WorkflowService workflowService, @Lazy WorkflowEngine engine) {
        this.workflowService = workflowService;
        this.engine = engine;
    }

    @Override
    public String getType() {
        return "whatsapp.receive";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        if (input == null || input.getItems() == null || input.getItems().isEmpty()) {
            return NodeResult.error(errorBatch("input message batch is empty"));
        }

        Map<String, Object> nodeResponse = input.getItems().get(0).getData();
        if (nodeResponse == null) {
            return NodeResult.error(errorBatch("node response is empty"));
        }

        Object bodyObj = nodeResponse.get("body");
        if (!(bodyObj instanceof Map)) {
            return NodeResult.error(errorBatch("body payload is invalid"));
        }

        Map<String, Object> bodyData = (Map<String, Object>) bodyObj;
        String contact = String.valueOf(bodyData.get("contact")).replace("whatsapp:", "");
        String message = String.valueOf(bodyData.get("message"));
        String action = "none";

        if (message.toLowerCase().contains("confirm") || message.toLowerCase().contains("reject")) {
            action = message.toLowerCase().contains("confirm") ? "approve" : "reject";
            String flowId = String.valueOf(nodeResponse.get("flowId"));
            final String finalAction = action;

            workflowService.open(flowId).flatMap(workflow -> {
                workflow.getDefinition().getNodes().stream()
                        .filter(node -> "human.approval".equals(node.getType()))
                        .findFirst()
                        .ifPresent(node -> {
                            MessageBatch out = new MessageBatch(
                                    List.of(
                                            new WorkflowMessage(
                                                    Map.of(
                                                            "step","approval",
                                                            "sessionId", contact,
                                                            "action", finalAction,
                                                            "message", message,
                                                            "flowId", flowId
                                                    )
                                            )
                                    )
                            );
                            engine.runFromNode(out, workflow.getDefinition(), node.getId(), flowId).subscribe();
                        });
                return Mono.just(workflow);
            }).subscribe();
        }

        MessageBatch out = new MessageBatch(
                List.of(
                        new WorkflowMessage(
                                Map.of(
                                        "contact", contact,
                                        "message", message,
                                        "action", action,
                                        "status", "received"
                                )
                        )
                )
        );

        return NodeResult.complected("success", out);
    }

    private MessageBatch errorBatch(String message) {
        return new MessageBatch(
                List.of(
                        new WorkflowMessage(Map.of("error", message))
                )
        );
    }
}
