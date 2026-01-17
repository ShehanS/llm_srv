package com.shehan.llmsvr.dtos;

import lombok.Data;

import java.util.Map;

@Data

public class NodeResult {

    public enum Status {
        COMPLETED,
        WAITING,
        FAILED,
        ERROR
    }

    private Status status;
    private String output;
    private MessageBatch messages;
    private Map<String, Object> waitPayload;

    public static NodeResult complected(String output, MessageBatch messages) {
        NodeResult r = new NodeResult();
        r.status = Status.COMPLETED;
        r.output = output;
        r.messages = messages;
        return r;
    }

    public static NodeResult waitFormApproval(Map<String, Object> waitPayload) {
        NodeResult r = new NodeResult();
        r.status = Status.WAITING;
        r.waitPayload = waitPayload;
        return r;
    }

    public static NodeResult error(MessageBatch messages) {
        NodeResult r = new NodeResult();
        r.status = Status.ERROR;
        r.messages = messages;
        r.output = "error";
        return r;
    }

}
