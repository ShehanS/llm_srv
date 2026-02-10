package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ApprovalDecisionRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AgentService {
    Mono<Object> conversation();

    Mono<Map<String, Object>> resume(String sessionId, ApprovalDecisionRequest approvalDecisionRequest);
}
