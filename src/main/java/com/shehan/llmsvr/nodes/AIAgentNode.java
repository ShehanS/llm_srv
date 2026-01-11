package com.shehan.llmsvr.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Component
public class AIAgentNode implements WorkflowNode {
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getType() {
        return "trigger.aiAgent";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception {

        WorkflowMessage first = input.getItems().get(0);
        Map<String, Object> data = first.getData();

        String sessionId = String.valueOf(
                data.getOrDefault("contact", UUID.randomUUID().toString())
        );

        String message = String.valueOf(
                data.getOrDefault("message", "")
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("messages", List.of(message));

        String url = NodeConfigUtil.getInputProp(config, "agentURL", "");

        Object response = webClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .block();

        log.info("Agent response: {}", response);

        return new NodeResult("success", input);
    }
}
