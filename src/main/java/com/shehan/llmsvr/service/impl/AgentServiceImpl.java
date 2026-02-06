package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.ApprovalDecisionRequest;
import com.shehan.llmsvr.dtos.PendingApproval;
import com.shehan.llmsvr.mcpTools.ApprovalTool;
import com.shehan.llmsvr.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Value("${intelligent-srv:http://localhost:8500}")
    private String intelligentServiceUrl;

    private final ApprovalTool approvalTool;

    @Qualifier("externalWebClient")
    private final WebClient webClient;

    @Override
    public Mono<Object> conversation() {
        return Mono.empty();
    }

    @Override
    public Mono<Map<String, Object>> resume(String sessionId, ApprovalDecisionRequest request) {
        log.info("Submitting decision for session: {} - action: {}", sessionId, request.getAction());

        Map<String, PendingApproval> pending = approvalTool.getPendingApprovals();
        PendingApproval targetApproval = null;
        String targetRequestId = null;

        for (Map.Entry<String, PendingApproval> entry : pending.entrySet()) {
            if (entry.getKey().startsWith(sessionId) && "pending".equals(entry.getValue().getStatus())) {
                targetApproval = entry.getValue();
                targetRequestId = entry.getKey();
                log.info("Found pending approval: {}", targetRequestId);
                break;
            }
        }

        if (targetApproval == null) {
            log.warn("No pending approval found for session: {}", sessionId);
            return Mono.just(Map.of(
                    "status", "error",
                    "error", "No pending approval found for session: " + sessionId,
                    "sessionId", sessionId
            ));
        }

        approvalTool.submitApprovalDecision(
                targetRequestId,
                request.getAction(),
                request.getFeedback()
        );

        String finalTargetRequestId = targetRequestId;
        Map<String, String> resumePayload = Map.of(
                "action", request.getAction(),
                "feedback", request.getFeedback() != null ? request.getFeedback() : ""
        );

        return webClient.post()
                .uri(intelligentServiceUrl + "/api/v1/conversation/resume/" + sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(resumePayload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(nodeResponse -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "success");
                    result.put("sessionId", sessionId);
                    result.put("requestId", finalTargetRequestId);
                    result.put("action", request.getAction());
                    result.put("workflowResumed", true);
                    result.put("response", nodeResponse);
                    return result;
                })
                .onErrorResume(error -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "error");
                    result.put("sessionId", sessionId);
                    result.put("requestId", finalTargetRequestId);
                    result.put("approvalRecorded", true);
                    result.put("workflowResumed", false);
                    result.put("error", error.getMessage());
                    return Mono.just(result);
                });
    }
}
