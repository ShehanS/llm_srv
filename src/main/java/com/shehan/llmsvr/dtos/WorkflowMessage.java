package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class WorkflowMessage {
    private Map<String, Object> data;
}
