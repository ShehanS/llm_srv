package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GmailNode implements WorkflowNode {

    @Override
    public String getType() {
        return "gmail.send";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        log.info("📧 Executing Gmail Node with config: {}", config);
                return NodeResult.complected("success", input);
    }
}
