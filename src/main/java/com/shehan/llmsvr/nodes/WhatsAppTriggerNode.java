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
                    output.add(new WorkflowMessage(Map.of("error", "AUTH_FAILED")));
                    continue;
                }
            }

            Map<String, Object> body =
                    (Map<String, Object>) data.get("body");

            if (body == null) {
                output.add(new WorkflowMessage(Map.of("error", "INVALID_BODY")));
                continue;
            }

            List<Map<String, Object>> entry =
                    (List<Map<String, Object>>) body.get("entry");

            if (entry == null || entry.isEmpty()) {
                output.add(new WorkflowMessage(Map.of("error", "INVALID_ENTRY")));
                continue;
            }

            List<Map<String, Object>> changes =
                    (List<Map<String, Object>>) entry.get(0).get("changes");

            if (changes == null || changes.isEmpty()) {
                output.add(new WorkflowMessage(Map.of("error", "INVALID_CHANGES")));
                continue;
            }

            Map<String, Object> value =
                    (Map<String, Object>) changes.get(0).get("value");

            if (value == null) {
                output.add(new WorkflowMessage(Map.of("error", "INVALID_VALUE")));
                continue;
            }

            String contact = null;

            List<Map<String, Object>> contacts =
                    (List<Map<String, Object>>) value.get("contacts");

            if (contacts != null && !contacts.isEmpty()) {
                contact = (String) contacts.get(0).get("wa_id");
            }

            List<Map<String, Object>> messages =
                    (List<Map<String, Object>>) value.get("messages");

            if (messages == null || messages.isEmpty()) {
                output.add(new WorkflowMessage(Map.of("error", "NO_MESSAGE")));
                continue;
            }

            Map<String, Object> message = messages.get(0);

            String msgFrom = (String) message.get("from");
            String timestampStr = (String) message.get("timestamp");
            Long time = timestampStr != null ? Long.valueOf(timestampStr) : null;

            String text = null;
            if ("text".equals(message.get("type"))) {
                Map<String, Object> textObj =
                        (Map<String, Object>) message.get("text");
                if (textObj != null) {
                    text = (String) textObj.get("body");
                }
            }

            if (contact == null) {
                contact = msgFrom;
            }

            if (contact == null || text == null || time == null) {
                output.add(new WorkflowMessage(Map.of("error", "INVALID_MESSAGE")));
                continue;
            }

            output.add(new WorkflowMessage(Map.of(
                    "contact", contact,
                    "message", text,
                    "time", time
            )));
        }

        return new NodeResult("default", new MessageBatch(output));
    }
}
