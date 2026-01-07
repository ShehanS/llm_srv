package com.shehan.llmsvr.service;

import com.shehan.llmsvr.nodes.WorkflowNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NodeRegistry {

    private final Map<String, WorkflowNode> nodes = new HashMap<>();

    public NodeRegistry(List<WorkflowNode> nodeList) {
        for (WorkflowNode node : nodeList) {
            nodes.put(node.getType(), node);
        }
    }

    public WorkflowNode get(String type) {
        return nodes.get(type);
    }
}
