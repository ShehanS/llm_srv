package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpTriggerNode implements WorkflowNode {

    @Override
    public String getType() {
        return "trigger.http";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        return new NodeResult("default", input);
    }
}
