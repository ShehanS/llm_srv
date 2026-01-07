package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class WebhookTriggerNode implements WorkflowNode {

    @Override
    public String getType() {
        return "trigger.webhook";
    }

    @Override
    public NodeResult execute(
            MessageBatch input,
            Map<String, Object> config
    ) {

        log.debug("WebhookTriggerNode input: {}", input);
        log.debug("WebhookTriggerNode config: {}", config);

        return new NodeResult("default", input);
    }
}
