package com.shehan.llmsvr.dtos;

import lombok.Data;

@Data
public class FlowEdge {
    private String source;
    private String target;
    private String sourceHandle;
}
