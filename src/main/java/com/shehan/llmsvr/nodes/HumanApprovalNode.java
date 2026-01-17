package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HumanApprovalNode implements WorkflowNode {
    @Value("${intelligent-srv.url}")
    private String intelligentSrvUrl;
    private final WebClient webClient = WebClient.builder().build();
    private int port;

    @Override
    public String getType() {
        return "human.approval";
    }

    @EventListener(WebServerInitializedEvent.class)
    public void init(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception {
        if (input == null || input.getItems() == null || input.getItems().isEmpty()) {
            log.error("HumanApprovalNode received empty input");
            return NodeResult.error(input);
        }

        WorkflowMessage first = input.getItems().get(0);
        Map<String, Object> inData = first.getData();
        String action = String.valueOf(inData.getOrDefault("action", "success"));
        String status = String.valueOf(inData.getOrDefault("status", ""));
        if (inData.containsKey("action")) {
            log.info("Internal Resume Processing: Human chose action [{}] with status [{}]", action, status);

            inData.put("node_processed_at", Instant.now().toString());
            inData.put("approval_status", "PROCESSED_BY_NODE");

            String sessionId = String.valueOf(inData.get("sessionId"));
            String agentUrl = intelligentSrvUrl + "/api/v1/approve/" + sessionId;

            Map<String, Object> actionRequest = new HashMap<>();
            boolean isApproved = status.toLowerCase().contains("approved") ? true : false;
            actionRequest.put("approved", isApproved);

            log.info("Notifying AI service at {}: approved={}", agentUrl, isApproved);

            try {
                Object response = webClient
                        .post()
                        .uri(agentUrl)
                        .bodyValue(actionRequest)
                        .retrieve()
                        .toEntity(Object.class)
                        .map(res -> res.getBody() != null ? res.getBody() : "SUCCESS")
                        .block();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode resJson = objectMapper.valueToTree(response);


                if ((resJson.get("status").asString().equals("completed"))&&(resJson.get("response").get("kwargs") == null)){
                    inData.put("response", response);
                    inData.put("status","error");
                    inData.put("message","Session id not valid or process already complected");
                    return NodeResult.error(new MessageBatch(List.of(new WorkflowMessage(inData))));

                }else{
                    String content =resJson.get("response").get("kwargs").get("content").asString();
                    inData.put("status","success");
                    inData.put("message", content);
                }

                log.info("AI service responded: {}", response);

            } catch (Exception e) {
                log.error("Failed to notify AI service: {}", e.getMessage());
                inData.put("status", "error");
                inData.put("message", "Session id not found");
                return NodeResult.error(new MessageBatch(List.of(new WorkflowMessage(inData))));
            }

            return NodeResult.complected(action, new MessageBatch(List.of(new WorkflowMessage(inData))));
        }
        Map<String, Object> inputContext = new HashMap<>();
        inputContext.put("input", inData);
        inputContext.put("body", inData.getOrDefault("body", inData));
        inputContext.put("headers", inData.getOrDefault("headers", Collections.emptyMap()));
        inputContext.put("all", inData);
        Map<String, Object> webhookPayload = new HashMap<>();

        String payloadSource = NodeConfigUtil.getMapperPayloadSource(config, "inputMapper", "");
        if (!payloadSource.isBlank() && inputContext.containsKey(payloadSource)) {
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
        Map<String, Object> outboundPayload = new HashMap<>();
        String flowId = String.valueOf(inData.getOrDefault("flowId", ""));
        String sessionId = String.valueOf(inData.getOrDefault("sessionId", ""));
        String samplePayload = """
                Sample Payload
                {
                    "sessionId": "<<SESSION_ID>>",
                    "action": "SUCCESS",
                    "status": "APPROVED || REJECTED",
                    
                }
                """;
        outboundPayload.put("callbackUrl", getBaseUrl() + "/service/approve/" + flowId);
        outboundPayload.put("sessionId", sessionId);
        outboundPayload.put("samplePayload", samplePayload);

        String webhookUrl = NodeConfigUtil.getInputProp(config, "outboundWebhookUrl", "");
        if (!webhookUrl.isBlank()) {
            log.info("Sending approval request to: {}", webhookUrl);
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(outboundPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            resp -> log.info("Approval webhook successfully delivered"),
                            err -> log.error("Failed to deliver approval webhook: {}", err.getMessage())
                    );
        }

        return NodeResult.waitFormApproval(Map.of(
                "message", "Awaiting human-in-the-loop decision",
                "requestSent", webhookPayload,
                "waitingSince", Instant.now().toString()
        ));
    }

    private String getBaseUrl() {
        return "http://" + getHost() + ":" + port;
    }

    private String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
