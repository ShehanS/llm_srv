package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConditionTextNode implements WorkflowNode {

    @Override
    public String getType() {
        return "condition.text";
    }

    @Override
    public NodeResult execute(
            MessageBatch input,
            Map<String, Object> config
    ) {
        String contains = (String) config.get("contains");

        boolean match = false;

        for (WorkflowMessage msg : input.getItems()) {
            Object text = msg.getData().get("message");
            if (text != null && text.toString().contains(contains)) {
                match = true;
                break;
            }
        }

        // output must match edge sourceHandle
        return new NodeResult(match ? "true" : "false", input);
    }
}
