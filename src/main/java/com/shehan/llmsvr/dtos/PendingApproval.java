package com.shehan.llmsvr.dtos;

import lombok.Data;

import java.util.concurrent.CompletableFuture;

@Data
public class PendingApproval {
    private final String requestId;
    private final String toolName;
    private final String toolArgs;
    private final String description;
    private final long timestamp;
    private String status;
    private String action;
    private String feedback;
    private boolean approved;
    private final CompletableFuture<String> completionFuture;

    public PendingApproval(String requestId, String toolName, String toolArgs, String description) {
        this.requestId = requestId;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
        this.approved = false;
        this.completionFuture = new CompletableFuture<>();
    }

    public void complete() {
        completionFuture.complete(action);
    }

    public String waitForDecision(long timeout, java.util.concurrent.TimeUnit unit) throws Exception {
        return completionFuture.get(timeout, unit);
    }

    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    public boolean isExpired(long maxAgeMillis) {
        return getAge() > maxAgeMillis;
    }
}
