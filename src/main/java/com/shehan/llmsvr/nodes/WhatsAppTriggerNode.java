package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class WhatsAppTriggerNode implements WorkflowNode {

    @Override
    public String getType() {
        return "trigger.whatsapp";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        String expectedToken = (String) config.get("verifyToken");
        String authHeaderName =
                (String) config.getOrDefault("authHeader", "X-API-KEY");

        List<WorkflowMessage> output = new ArrayList<>();

        for (WorkflowMessage inMsg : input.getItems()) {

            Map<String, Object> data = inMsg.getData();
            if (expectedToken != null) {
                Map<String, Object> headers =
                        (Map<String, Object>) data.get("headers");

                String receivedToken =
                        headers != null
                                ? String.valueOf(headers.get(authHeaderName))
                                : null;

                if (!expectedToken.equals(receivedToken)) {
                    log.warn("WhatsApp auth failed");
                    continue;
                }
            }
            List<Map<String, Object>> entry =
                    (List<Map<String, Object>>) data.get("entry");
            if (entry == null || entry.isEmpty()) continue;

            List<Map<String, Object>> changes =
                    (List<Map<String, Object>>) entry.get(0).get("changes");
            if (changes == null || changes.isEmpty()) continue;

            Map<String, Object> value =
                    (Map<String, Object>) changes.get(0).get("value");

            List<Map<String, Object>> messages =
                    (List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) continue;

            Map<String, Object> message = messages.get(0);

            String from = (String) message.get("from");
            String text = null;

            if ("text".equals(message.get("type"))) {
                Map<String, Object> textObj =
                        (Map<String, Object>) message.get("text");
                text = (String) textObj.get("body");
            }

            if (from == null || text == null) continue;

            Map<String, Object> out = Map.of(
                    "channel", "whatsapp",
                    "from", from,
                    "text", text
            );

            output.add(new WorkflowMessage(out));
        }

        return new NodeResult("default", new MessageBatch(output));
    }
}
