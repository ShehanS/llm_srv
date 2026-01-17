package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class HumanApprovalNode implements WorkflowNode {
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getType() {
        return "human.approval";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception {
        if (input == null || input.getItems() == null || input.getItems().isEmpty()) {
            return NodeResult.error(input);
        }

        WorkflowMessage first = input.getItems().get(0);
        Map<String, Object> inData = first.getData();


        Map<String, Object> inputContext = new HashMap<>();
        inputContext.put("input", inData);
        inputContext.put("body", inData.getOrDefault("body", inData));
        inputContext.put("headers", inData.getOrDefault("headers", Collections.emptyMap()));
        inputContext.put("query", inData.getOrDefault("query", Collections.emptyMap()));
        inputContext.put("all", inData);

        Map<String, Object> webhookPayload = new HashMap<>();

        String payloadSource = NodeConfigUtil.getMapperPayloadSource(config, "inputMapper", "");
        if (!payloadSource.isBlank()) {
            Object sourceData = inputContext.get(payloadSource);
            if (sourceData instanceof Map) {
                webhookPayload.putAll((Map<String, Object>) sourceData);
            }
        }

        Map<String, Object> resolvedInput = ExpressionResolver.resolve(
                config,
                "inputMapper",
                inputContext,
                new HashMap<>()
        );
        if (resolvedInput != null) webhookPayload.putAll(resolvedInput);

        String webhookUrl = NodeConfigUtil.getInputProp(config, "webhookUrl", "");
        if (!webhookUrl.isBlank()) {
            log.info("Triggering approval webhook: {}", webhookUrl);
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(webhookPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe();
        }

        return NodeResult.waitFormApproval(Map.of(
                "message", "Waiting for external webhook response",
                "requestSent", webhookPayload,
                "originalAiResponse", inData
        ));
    }
}
