package com.shehan.llmsvr.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.mcpTools.ApprovalTool;
import com.shehan.llmsvr.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HumanApprovalNode implements WorkflowNode {

    private final AgentService agentService;
    private final ApprovalTool approvalTool;
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int port;

    public HumanApprovalNode(AgentService agentService, ApprovalTool approvalTool) {
        this.agentService = agentService;
        this.approvalTool = approvalTool;
    }

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
            return NodeResult.error(input);
        }

        WorkflowMessage first = input.getItems().get(0);
        Map<String, Object> inData = first.getData();
        String sessionId = String.valueOf(inData.get("sessionId"));
        String userText = String.valueOf(inData.getOrDefault("body", "")).trim();

        if (userText.equalsIgnoreCase("Confirm") || userText.equalsIgnoreCase("Reject")) {
            PendingApproval pending = approvalTool.getPendingApprovalForSession(sessionId);

            if (pending != null) {
                String decision = userText.equalsIgnoreCase("Confirm") ? "approve" : "reject";
                ApprovalDecisionRequest actionRequest = new ApprovalDecisionRequest();
                actionRequest.setAction(decision);

                try {
                    Map<String, Object> response = agentService.resume(sessionId, actionRequest).block();
                    approvalTool.submitApprovalDecision(pending.getRequestId(), decision, "WhatsApp Decision");

                    JsonNode resJson = objectMapper.valueToTree(response);
                    JsonNode aiNode = resJson.path("response");

                    String content = aiNode.path("response").path("kwargs").path("content").asText();
                    if (content.isEmpty()) {
                        content = aiNode.path("response").path("content").asText();
                    }
                    if (content.isEmpty()) {
                        content = aiNode.path("agentResponse").path("kwargs").path("content").asText();
                    }

                    Map<String, Object> mutableData = new HashMap<>(inData);
                    mutableData.put("status", "success");
                    mutableData.put("message", content);
                    mutableData.put("node_processed_at", Instant.now().toString());

                    return NodeResult.complected("success", new MessageBatch(List.of(new WorkflowMessage(mutableData))));
                } catch (Exception e) {
                    log.error("Resume failed for session {}: {}", sessionId, e.getMessage());
                    return NodeResult.error(input);
                }
            }
        }

        String aiMessage = extractAiContent(inData);
        processNotifications(config, sessionId, aiMessage);

        return NodeResult.waitFormApproval(Map.of(
                "status", "awaiting_human_decision",
                "sessionId", sessionId,
                "timestamp", Instant.now().toString()
        ));
    }

    private String extractAiContent(Map<String, Object> inData) {
        try {
            Object resp = inData.get("response");
            if (resp instanceof Map) {
                Map<String, Object> kwargs = (Map<String, Object>) ((Map<String, Object>) resp).get("kwargs");
                if (kwargs != null && kwargs.containsKey("content")) {
                    return String.valueOf(kwargs.get("content"));
                }
            }
        } catch (Exception e) {}
        return "A sensitive operation requires your approval.";
    }

    private void processNotifications(Map<String, Object> config, String sessionId, String content) {
        List<Map<String, Object>> approvals = (List<Map<String, Object>>) config.get("approval");
        if (approvals == null) return;
        for (Map<String, Object> method : approvals) {
            if ("whatapp".equals(method.get("service"))) {
                sendWhatsApp(config, sessionId, content);
            }
        }
    }

    private void sendWhatsApp(Map<String, Object> config, String sessionId, String bodyContent) {
        String accountId = String.valueOf(config.get("accountId"));
        String token = String.valueOf(config.get("token"));
        String from = String.valueOf(config.get("from"));
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountId + "/Messages.json";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", "whatsapp:" + sessionId);
        formData.add("From", "whatsapp:" + from);
        formData.add("Body", "⚠️ *Approval Required*\n\n" + bodyContent + "\n\nReply *Confirm* to proceed or *Reject* to cancel.");

        webClient.post()
                .uri(url)
                .headers(h -> h.setBasicAuth(accountId, token))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .toBodilessEntity()
                .subscribe();
    }

    private String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
