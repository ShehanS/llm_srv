package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
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
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            WorkflowMessage first = input.getItems().get(0);
            Map<String, Object> inData = first.getData();

            String sessionId =
                    String.valueOf(inData.getOrDefault("contact", UUID.randomUUID().toString()));

            String message =
                    String.valueOf(inData.getOrDefault("message", ""));

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("sessionId", sessionId);
            requestPayload.put("messages", List.of(message));

            String url = NodeConfigUtil.getInputProp(config, "agentURL", "");

            Object response = webClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            Map<String, Object> responseData;
            if (response instanceof Map<?, ?>) {
                responseData = new HashMap<>((Map<String, Object>) response);
            } else {
                responseData = Map.of("data", response);
            }

            // Extract mapper configuration
            Map<String, Object> mapper =
                    NodeConfigUtil.getInputPropMapper(config, "mapper", new HashMap<>());

            String payloadSource =
                    NodeConfigUtil.getMapperPayloadSource(config, "mapper", "body");

            String payloadExpression =
                    NodeConfigUtil.getMapperPayloadExpression(config, "mapper", "");

            List<Map<String, String>> mappings =
                    NodeConfigUtil.getMapperMap(config, "mapper", new ArrayList<>());

            // Apply transformation with priority: mappings > expression > source
            Object payload;

            if (mappings != null && !mappings.isEmpty()) {
                payload = applyMapper(mappings, responseData, inData);
                log.debug("Using Object Mapper with {} mappings", mappings.size());

            } else if (payloadExpression != null && !payloadExpression.isBlank()) {
                payload = ExpressionResolver.resolve(
                        payloadExpression,
                        Map.of(
                                "body", responseData,
                                "input", inData,
                                "all", responseData
                        )
                );
                log.debug("Using Payload Expression: {}", payloadExpression);

            } else {
                payload = switch (payloadSource) {
                    case "input" -> new HashMap<>(inData);
                    case "all" -> new HashMap<>(responseData);
                    default -> responseData;
                };
                log.debug("Using Payload Source: {}", payloadSource);
            }

            Map<String, Object> out;
            if (payload instanceof Map) {
                out = new HashMap<>((Map<String, Object>) payload);
            } else {
                out = new HashMap<>();
                out.put("data", payload);
            }

            return new NodeResult(
                    "success",
                    new MessageBatch(List.of(new WorkflowMessage(out)))
            );

        } catch (Exception e) {
            log.error("AI Agent error", e);
            return new NodeResult("error", input);
        }
    }

    private Map<String, Object> applyMapper(
            List<Map<String, String>> mapper,
            Map<String, Object> body,
            Map<String, Object> input
    ) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> context = Map.of(
                "body", body,
                "input", input,
                "all", body
        );

        for (Map<String, String> mapping : mapper) {
            String key = mapping.get("key");
            String valueExpression = mapping.get("value");

            if (key == null || key.isBlank()) {
                continue;
            }

            Object resolvedValue;
            if (valueExpression != null
                    && valueExpression.startsWith("{{")
                    && valueExpression.endsWith("}}")) {
                resolvedValue = ExpressionResolver.resolve(valueExpression, context);
            } else {
                resolvedValue = valueExpression;
            }

            result.put(key, resolvedValue);
        }

        return result;
    }
}
