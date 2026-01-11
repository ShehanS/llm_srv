package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
public class WorkflowDefinition {
    private List<FlowNode> nodes;
    private List<FlowEdge> edges;
}
