package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeResult {
    private String output;
    private MessageBatch messages;
}
