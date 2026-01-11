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
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        try {
            WorkflowMessage in = input.getItems().get(0);
            Map<String, Object> data = in.getData();

            String allowedMethods =
                    NodeConfigUtil.getInputProp(config, "method", "GET");

            String mediaType =
                    NodeConfigUtil.getInputProp(config, "mediaType", "application/json");

            Map<String, Object> mapper = NodeConfigUtil.getInputPropMapper(config, "mapper", new HashMap<>());

            String payloadSource = NodeConfigUtil.getMapperPayloadSource(config, "mapper", "body");
            String payloadExpression = NodeConfigUtil.getMapperPayloadExpression(config, "mapper", "");
            List<Map<String, String>> mappings = NodeConfigUtil.getMapperMap(config, "mapper", new ArrayList<>());

            String method = String.valueOf(data.get("method"));
            Map<String, Object> body = castMap(data.get("body"));
            Map<String, String> headers = castStringMap(data.get("headers"));
            Map<String, String> query = castStringMap(data.get("query"));

            if (!isMethodAllowed(method, allowedMethods)) {
                log.warn("HTTP method not allowed: {}", method);
                return new NodeResult("error", input);
            }

            Object payload;
            if (mappings != null && !mappings.isEmpty()) {
                payload = applyMapper(mappings, body, headers, query, data);
                log.debug("Using Object Mapper with {} mappings", mappings.size());

            } else if (payloadExpression != null && !payloadExpression.isBlank()) {
                payload = ExpressionResolver.resolve(
                        payloadExpression,
                        Map.of(
                                "body", body,
                                "headers", headers,
                                "query", query,
                                "all", data
                        )
                );
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
                out.put("data", payload);
            }

            return new NodeResult(
                    "default",
                    new MessageBatch(List.of(new WorkflowMessage(out)))
            );

        } catch (Exception e) {
            log.error("HTTP trigger error", e);
            return new NodeResult("error", input);
        }
    }

    private Map<String, Object> applyMapper(
            List<Map<String, String>> mapper,
            Map<String, Object> body,
            Map<String, String> headers,
            Map<String, String> query,
            Map<String, Object> allData
    ) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> context = Map.of(
                "body", body,
                "headers", headers,
                "query", query,
                "all", allData
        );

        for (Map<String, String> mapping : mapper) {
            String key = mapping.get("key");
            String valueExpression = mapping.get("value");

            if (key == null || key.isBlank()) {
                continue;
            }

            Object resolvedValue;
            if (valueExpression != null && valueExpression.startsWith("{{") && valueExpression.endsWith("}}")) {
                resolvedValue = ExpressionResolver.resolve(valueExpression, context);
            } else {
                resolvedValue = valueExpression;
            }

            result.put(key, resolvedValue);
        }

        return result;
    }

    private boolean isMethodAllowed(String method, String allowed) {
        return Arrays.stream(allowed.split(","))
                .map(String::trim)
                .anyMatch(m -> m.equalsIgnoreCase(method));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castStringMap(Object o) {
        if (!(o instanceof Map<?, ?> m)) return Map.of();
        Map<String, String> result = new HashMap<>();
        m.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }
}
