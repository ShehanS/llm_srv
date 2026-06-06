package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class ExecutionStep {
    private FlowNode node;
    private MessageBatch messages;
}
