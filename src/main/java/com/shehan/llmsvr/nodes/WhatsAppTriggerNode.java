package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WhatsAppTriggerNode implements WorkflowNode {

    @Override
    public String getType() {
        return "trigger.whatsapp";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        log.debug("WhatsAppNode input: {}", input);
        log.debug("WhatsAppNode config: {}", config);
        List<WorkflowMessage> msg = new ArrayList<>();
        WorkflowMessage workflowMessage = new WorkflowMessage(Map.of("test","value"));
        msg.add(workflowMessage);

        return new NodeResult("default", new MessageBatch(msg));
    }
}
