package com.shehan.llmsvr.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTriggerRequest {
    private Map<String, Object> payload;
    private WorkflowDefinition workflow;
}
