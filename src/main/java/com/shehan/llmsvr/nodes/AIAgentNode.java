package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.MapStructureDebugger;
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
    @Value("${intelligent-srv.url}")
    private String intelligentSrvUrl;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getType() {
        return "trigger.aiAgent";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            WorkflowMessage first = input.getItems().get(0);
            Map<String, Object> inData = first.getData();
            Map<String, Object> inputContext = Map.of(
                    "input", inData,
                    "body", inData,
                    "all", inData
            );
            Map<String, Object> requestPayload =
                    buildAgentRequestPayload(config, inputContext, inData);

            log.info("FINAL AI REQUEST PAYLOAD: {}", requestPayload);

            String url = NodeConfigUtil.getInputProp(config, "agentURL", "");
            String routeAgent = NodeConfigUtil.getInputProp(config, "routeAgent", "");
            String configUrl = intelligentSrvUrl + "/api/v1/fetch-config/" + routeAgent;

            Object responseFetchConfig = webClient
                    .get()
                    .uri(configUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            log.info("Reload config response {}", responseFetchConfig);


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

            Map<String, Object> responseData =
                    response instanceof Map
                            ? new HashMap<>((Map<String, Object>) response)
                            : Map.of("data", response);

            MapStructureDebugger.printStructure(responseData);

            Map<String, Object> outputContext = Map.of(
                    "body", responseData,
                    "input", inData,
                    "all", responseData
            );

            Map<String, Object> out = resolveMapper(
                    config,
                    "outputMapper",
                    outputContext,
                    responseData
            );

            System.out.println(out);
            return new NodeResult(
                    "success",
                    new MessageBatch(List.of(new WorkflowMessage(out)))
            );


        } catch (Exception e) {
            log.error("AI Agent error", e);
            return new NodeResult("error", input);
        }
    }

    private Map<String, Object> buildAgentRequestPayload(
            Map<String, Object> config,
            Map<String, Object> inputContext,
            Map<String, Object> inData
    ) {

        Map<String, Object> payload = new HashMap<>(defaultInputPayload(inData));

        List<Map<String, String>> mappings =
                NodeConfigUtil.getMapperMap(config, "inputMapper", Collections.emptyList());

        for (Map<String, String> mapping : mappings) {
            String key = mapping.get("key");
            String valueExpr = mapping.get("value");

            if (key == null || key.isBlank()) continue;

            Object resolved;
            if (valueExpr != null && valueExpr.startsWith("{{") && valueExpr.endsWith("}}")) {
                resolved = ExpressionResolver.resolve(valueExpr, inputContext);
            } else {
                resolved = valueExpr;
            }

            payload.put(key, resolved);
        }
        normalizeAgentPayload(payload);

        return payload;
    }

    private Map<String, Object> defaultInputPayload(Map<String, Object> inData) {

        String sessionId =
                String.valueOf(inData.getOrDefault("sessionId", UUID.randomUUID().toString()));

        Object messageValue = inData.get("message");

        List<String> messages;
        if (messageValue instanceof List<?> list) {
            messages = new ArrayList<>();
            for (Object item : list) {
                messages.add(String.valueOf(item));
            }
        } else if (messageValue != null) {
            messages = List.of(String.valueOf(messageValue));
        } else {
            messages = List.of("");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", messages);
        payload.put("sessionId", sessionId);

        return payload;
    }

    @SuppressWarnings("unchecked")
    private void normalizeAgentPayload(Map<String, Object> payload) {
        Object messagesObj = payload.get("messages");

        if (messagesObj instanceof List<?> list) {
            List<String> normalized = new ArrayList<>();
            for (Object o : list) {
                normalized.add(String.valueOf(o));
            }
            payload.put("messages", normalized);
        } else if (messagesObj != null) {
            payload.put("messages", List.of(String.valueOf(messagesObj)));
        } else {
            payload.put("messages", List.of(""));
        }
        payload.putIfAbsent("sessionId", UUID.randomUUID().toString());
    }

    private Map<String, Object> resolveMapper(
            Map<String, Object> config,
            String mapperName,
            Map<String, Object> context,
            Map<String, Object> fallback
    ) {
        List<Map<String, String>> mappings =
                NodeConfigUtil.getMapperMap(config, mapperName, Collections.emptyList());

        if (mappings == null || mappings.isEmpty()) {
            return new HashMap<>(fallback);
        }

        Map<String, Object> result = new HashMap<>();

        for (Map<String, String> mapping : mappings) {
            String key = mapping.get("key");
            String valueExpr = mapping.get("value");

            if (key == null || key.isBlank()) continue;

            Object resolved =
                    (valueExpr != null && valueExpr.startsWith("{{") && valueExpr.endsWith("}}"))
                            ? ExpressionResolver.resolve(valueExpr, context)
                            : valueExpr;

            result.put(key, resolved);
        }

        return result;
    }
}
