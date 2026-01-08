package com.shehan.llmsvr.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class FlowNode {
    private String id;
    private String type;
    private String label;
    private String color;
    private Position position;
    private Map<String, Object> config;
}
