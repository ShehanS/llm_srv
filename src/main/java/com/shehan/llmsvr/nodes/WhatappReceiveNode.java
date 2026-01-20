package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WhatappReceiveNode implements WorkflowNode {

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

        String contact = String.valueOf(bodyData.get("contact"));
        String message = String.valueOf(bodyData.get("message"));

        MessageBatch out = new MessageBatch(
                List.of(
                        new WorkflowMessage(
                                Map.of("contact", contact, "message", message)
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
