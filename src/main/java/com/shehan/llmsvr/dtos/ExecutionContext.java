package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {
    private FlowNode node;
    private MessageBatch messages;
    private String runId;
    private Integer attempt;
}
