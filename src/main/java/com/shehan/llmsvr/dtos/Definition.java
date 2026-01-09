package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Definition {
    private List<FlowNode> nodes;
    private List<FlowEdge> edges;
}
