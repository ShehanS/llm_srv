package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class HttpTriggerNode implements WorkflowNode {

    @Override
    public String getType() {
        return "trigger.http";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            WorkflowMessage in = input.getItems().get(0);
            Map<String, Object> data = in.getData();

            String allowedMethods = NodeConfigUtil.getInputProp(config, "method", "GET");
            String payloadSource = NodeConfigUtil.getMapperPayloadSource(config, "mapper", "body");
            String payloadExpression = NodeConfigUtil.getMapperPayloadExpression(config, "mapper", "");

            String method = String.valueOf(data.get("method"));
            Map<String, Object> body = castMap(data.get("body"));
            Map<String, String> headers = castStringMap(data.get("headers"));
            Map<String, String> query = castStringMap(data.get("query"));

            if (!isMethodAllowed(method, allowedMethods)) {
                log.warn("HTTP method not allowed: {}", method);
                return NodeResult.error(input);
            }

            Map<String, Object> context = Map.of(
                    "body", body,
                    "headers", headers,
                    "query", query,
                    "all", data
            );

            Object payload;
            Map<String, Object> resolvedMappings = ExpressionResolver.resolve(config, "mapper", context, null);

            if (resolvedMappings != null && !resolvedMappings.isEmpty()) {
                payload = resolvedMappings;
                log.debug("Using Object Mapper with resolved mappings");
            } else if (payloadExpression != null && !payloadExpression.isBlank()) {
                payload = ExpressionResolver.resolve(payloadExpression, context);
                log.debug("Using Payload Expression: {}", payloadExpression);
            } else {
                payload = switch (payloadSource) {
                    case "headers" -> new HashMap<>(headers);
                    case "query" -> new HashMap<>(query);
                    case "all" -> new HashMap<>(data);
                    default -> body;
                };
                log.debug("Using Payload Source: {}", payloadSource);
            }

            Map<String, Object> out;
            if (payload instanceof Map) {
                out = new HashMap<>((Map<String, Object>) payload);
            } else {
                out = new HashMap<>();
                out.put("data", payload != null ? payload : Collections.emptyMap());
            }

            return NodeResult.complected("default", new MessageBatch(List.of(new WorkflowMessage(out))));

        } catch (Exception e) {
            log.error("HTTP trigger error", e);
            return NodeResult.error(input);
        }
    }

    private boolean isMethodAllowed(String method, String allowed) {
        return Arrays.stream(allowed.split(","))
                .map(String::trim)
                .anyMatch(m -> m.equalsIgnoreCase(method));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castStringMap(Object o) {
        if (!(o instanceof Map<?, ?> m)) return new HashMap<>();
        Map<String, String> result = new HashMap<>();
        m.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }
}
