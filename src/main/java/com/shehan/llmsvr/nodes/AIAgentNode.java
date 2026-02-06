package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            WorkflowMessage first = input.getItems().get(0);
            Map<String, Object> inData = first.getData();

            Map<String, Object> inputContext = new HashMap<>();
            inputContext.put("input", inData);
            inputContext.put("body", inData.getOrDefault("body", inData));
            inputContext.put("headers", inData.getOrDefault("headers", Collections.emptyMap()));
            inputContext.put("query", inData.getOrDefault("query", Collections.emptyMap()));
            inputContext.put("all", inData);


            Map<String, Object> requestPayload = buildAgentRequestPayload(config, inputContext, inData);

            log.info("Final AI Request Payload: {}", requestPayload);

            String url = NodeConfigUtil.getInputProp(config, "agentURL", "");
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("Agent URL is required");
            }

            Object response = webClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            Map<String, Object> responseData = response instanceof Map
                    ? (Map<String, Object>) response
                    : Map.of("data", response != null ? response : Collections.emptyMap());
            if (Boolean.TRUE.equals(responseData.get("requires_approval")) ||
                    "awaiting_approval".equals(responseData.get("status"))) {
                responseData.put("flowId", String.valueOf(inData.get("flowId")));
                MessageBatch nextBatch = new MessageBatch(List.of(new WorkflowMessage(responseData)));
                return NodeResult.complected("action", nextBatch);
            }

            Map<String, Object> outputContext = Map.of(
                    "body", responseData,
                    "input", inData,
                    "all", responseData
            );

            Map<String, Object> out = ExpressionResolver.resolve(config, "outputMapper", outputContext, responseData);

            return NodeResult.complected("success", new MessageBatch(List.of(new WorkflowMessage(out))));

        } catch (Exception e) {
            log.error("AI Agent communication failed: {}", e.getMessage());
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "error");
            errorDetails.put("message", e.getMessage());
            return NodeResult.error(new MessageBatch(List.of(new WorkflowMessage(errorDetails))));
        }
    }

    private Map<String, Object> buildAgentRequestPayload(
            Map<String, Object> config,
            Map<String, Object> inputContext,
            Map<String, Object> inData
    ) {
        Map<String, Object> finalPayload = new HashMap<>();
        String payloadSource = NodeConfigUtil.getMapperPayloadSource(config, "inputMapper", "");
        if (!payloadSource.isBlank()) {
            Object sourceData = inputContext.get(payloadSource);
            if (sourceData instanceof Map) {
                finalPayload.putAll((Map<String, Object>) sourceData);
            }
        }
        Map<String, Object> resolvedMappings = ExpressionResolver.resolve(
                config,
                "inputMapper",
                inputContext,
                new HashMap<>()
        );

        if (resolvedMappings != null) {
            finalPayload.putAll(resolvedMappings);
        }

        normalizeAgentPayload(finalPayload, inData);

        return finalPayload;
    }

    private void normalizeAgentPayload(Map<String, Object> payload, Map<String, Object> inData) {
        Object messagesObj = payload.get("messages");
        List<String> normalizedMessages = new ArrayList<>();

        if (messagesObj instanceof List<?> list) {
            list.forEach(item -> normalizedMessages.add(String.valueOf(item)));
        } else if (messagesObj != null && !String.valueOf(messagesObj).isBlank()) {
            normalizedMessages.add(String.valueOf(messagesObj));
        } else {
            Object fallbackMsg = inData.get("message");
            normalizedMessages.add(fallbackMsg != null ? String.valueOf(fallbackMsg) : "");
        }
        payload.put("messages", normalizedMessages);

        Object sid = payload.get("sessionId");
        if (sid == null || String.valueOf(sid).isBlank()) {
            payload.put("sessionId", inData.getOrDefault("sessionId", UUID.randomUUID().toString()));
        } else {
            payload.put("sessionId", String.valueOf(sid));
        }
    }
}
