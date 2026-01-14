package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace {

    private String runId;
    private String nodeId;
    private String nodeType;
    private Object input;
    private Object output;
    private Map<String, Object> config;
    private Status status;
    private Instant startedAt;
    private Instant completedAt;
    private String error;

    public enum Status {
        PENDING,
        RUNNING,
        ERROR,
        COMPLETE
    }
}
