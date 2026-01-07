package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionTrace {

    private String runId;
    private String nodeId;
    private String nodeType;

    private Map<String, Object> input;
    private Map<String, Object> output;

    private Map<String, Object> config;
    private String status;

    private Instant startedAt;
    private Instant finishedAt;
    private String error;
}
