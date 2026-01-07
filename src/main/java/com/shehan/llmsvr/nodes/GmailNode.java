package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GmailNode implements WorkflowNode {

    @Override
    public String getType() {
        return "gmail.send";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        System.out.println("📧 Gmail config → " + config);
        return new NodeResult("success", input);
    }
}
