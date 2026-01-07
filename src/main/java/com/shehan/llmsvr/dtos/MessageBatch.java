package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MessageBatch {
    private List<WorkflowMessage> items;
}
