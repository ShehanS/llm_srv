package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class WhatsAppNode implements WorkflowNode {

    @Override
    public String getType() {
        return "whatsapp.send";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        for (WorkflowMessage msg : input.getItems()) {
           log.info("📱 WhatsApp → " + msg.getData());
        }

        return new NodeResult("success", input);
    }
}
