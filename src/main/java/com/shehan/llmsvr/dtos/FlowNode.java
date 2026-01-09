package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowNode {
    private String id;
    private String type;
    private String label;
    private String color;
    private Position position;
    private Map<String, Object> config;
    private List<Output> outputs;
    private List<Input> inputs;

}
