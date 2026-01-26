package com.shehan.llmsvr.mcpTools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApprovalTool {

    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();

    @Tool(name = "request_approval", description = "Request human approval before executing a sensitive operation")
    public Map<String, Object> requestApproval(
            @ToolParam(description = "Unique identifier for this approval request") String requestId,
            @ToolParam(description = "Name of the tool that requires approval") String toolName,
            @ToolParam(description = "Arguments that will be passed to the tool") String toolArgs,
            @ToolParam(description = "Description of what this operation will do") String description
    ) {
        PendingApproval approval = new PendingApproval(requestId, toolName, toolArgs, description);
        pendingApprovals.put(requestId, approval);

        log.info("Approval request created: {} for tool: {}", requestId, toolName);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "pending");
        response.put("requestId", requestId);
        response.put("message", "Approval request created. Waiting for human decision.");
        response.put("toolName", toolName);
        response.put("toolArgs", toolArgs);
        response.put("description", description);

        return response;
    }

    @Tool(name = "check_approval_status", description = "Check the status of a pending approval request")
    public Map<String, Object> checkApprovalStatus(
            @ToolParam(description = "The request ID to check") String requestId
    ) {
        PendingApproval approval = pendingApprovals.get(requestId);

        Map<String, Object> response = new HashMap<>();
        if (approval == null) {
            response.put("status", "not_found");
            response.put("message", "No approval request found with this ID");
            return response;
        }

        response.put("status", approval.getStatus());
        response.put("requestId", requestId);
        response.put("toolName", approval.getToolName());
        response.put("approved", approval.isApproved());

        if (approval.getStatus().equals("completed")) {
            response.put("action", approval.getAction());
            response.put("feedback", approval.getFeedback());
        }

        return response;
    }

    public Map<String, Object> submitApprovalDecision(String requestId, String action, String feedback) {
        PendingApproval approval = pendingApprovals.get(requestId);

        Map<String, Object> response = new HashMap<>();
        if (approval == null) {
            log.warn("Approval request not found: {}", requestId);
            response.put("status", "error");
            response.put("message", "No approval request found with this ID");
            return response;
        }

        approval.setAction(action);
        approval.setFeedback(feedback);
        approval.setStatus("completed");
        approval.setApproved("approve".equals(action));
        approval.complete();

        log.info("Approval decision recorded: {} - {}", requestId, action);

        response.put("status", "success");
        response.put("requestId", requestId);
        response.put("action", action);
        response.put("message", "Approval decision recorded");

        return response;
    }

    public Map<String, PendingApproval> getPendingApprovals() {
        return new HashMap<>(pendingApprovals);
    }

    public List<PendingApproval> getPendingApprovalsBySession(String sessionId) {
        return pendingApprovals.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId + "_"))
                .filter(entry -> "pending".equals(entry.getValue().getStatus()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public PendingApproval getPendingApprovalForSession(String sessionId) {
        return pendingApprovals.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId + "_"))
                .filter(entry -> "pending".equals(entry.getValue().getStatus()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public void clearApproval(String requestId) {
        PendingApproval removed = pendingApprovals.remove(requestId);
        if (removed != null) {
            log.info("Approval request cleared: {}", requestId);
        }
    }

    public void clearCompletedApprovals() {
        List<String> toRemove = pendingApprovals.entrySet().stream()
                .filter(entry -> "completed".equals(entry.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(pendingApprovals::remove);
        log.info("Cleared {} completed approval requests", toRemove.size());
    }

    public void clearSessionApprovals(String sessionId) {
        List<String> toRemove = pendingApprovals.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId + "_"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(pendingApprovals::remove);
        log.info("Cleared {} approval requests for session: {}", toRemove.size(), sessionId);
    }

    public long getPendingCount() {
        return pendingApprovals.values().stream()
                .filter(approval -> "pending".equals(approval.getStatus()))
                .count();
    }

    public long getCompletedCount() {
        return pendingApprovals.values().stream()
                .filter(approval -> "completed".equals(approval.getStatus()))
                .count();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", pendingApprovals.size());
        stats.put("pending", getPendingCount());
        stats.put("completed", getCompletedCount());
        stats.put("approved", pendingApprovals.values().stream()
                .filter(PendingApproval::isApproved)
                .count());
        stats.put("rejected", pendingApprovals.values().stream()
                .filter(approval -> "completed".equals(approval.getStatus()) && !approval.isApproved())
                .count());
        return stats;
    }

    public static class PendingApproval {
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

        public String waitForDecision(long timeout, TimeUnit unit) throws Exception {
            return completionFuture.get(timeout, unit);
        }

        public String getRequestId() { return requestId; }
        public String getToolName() { return toolName; }
        public String getToolArgs() { return toolArgs; }
        public String getDescription() { return description; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getAction() { return action; }
        public String getFeedback() { return feedback; }
        public boolean isApproved() { return approved; }

        public void setStatus(String status) { this.status = status; }
        public void setAction(String action) { this.action = action; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        public void setApproved(boolean approved) { this.approved = approved; }

        public long getAge() {
            return System.currentTimeMillis() - timestamp;
        }

        public boolean isExpired(long maxAgeMillis) {
            return getAge() > maxAgeMillis;
        }
    }
}
