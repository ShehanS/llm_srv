package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WhatappSendNode implements WorkflowNode {

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getType() {
        return "whatsapp.send";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        String accountId = NodeConfigUtil.getInputProp(config, "accountId", "");
        String token = NodeConfigUtil.getInputProp(config, "token", "");
        String to = NodeConfigUtil.getInputProp(config, "to", "");
        String from = NodeConfigUtil.getInputProp(config, "from", "");

        if (isBlank(accountId)) return NodeResult.error(errorBatch("accountId cannot be empty"));
        if (isBlank(token)) return NodeResult.error(errorBatch("token cannot be empty"));
        if (isBlank(from)) return NodeResult.error(errorBatch("from cannot be empty"));

        Map<String, Object> resJson = input.getItems().get(0).getData();

        String sessionId = resJson.get("sessionId") != null
                ? String.valueOf(resJson.get("sessionId"))
                : null;

        String finalTo = !isBlank(to) ? to : sessionId;

        if (!isValidPhone(finalTo)) {
            return skipped("invalid sessionId or phone number");
        }

        String body = extractBody(resJson);
        if (isBlank(body)) {
            return skipped("empty message body");
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/" +
                accountId + "/Messages.json";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", "whatsapp:" + finalTo);
        formData.add("From", "whatsapp:" + from);
        formData.add("Body", body);

        try {
            webClient.post()
                    .uri(url)
                    .headers(h -> h.setBasicAuth(accountId, token))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .filter(ex ->
                                            ex instanceof WebClientResponseException.TooManyRequests
                                    )
                    )
                    .block();

            return success(finalTo, from);

        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("Twilio rate limited (429). Message deferred.");
            return NodeResult.complected(
                    "retry",
                    new MessageBatch(
                            List.of(
                                    new WorkflowMessage(
                                            Map.of(
                                                    "status", "retry",
                                                    "reason", "twilio_rate_limited",
                                                    "provider", "twilio"
                                            )
                                    )
                            )
                    )
            );

        } catch (Exception e) {
            log.error("WhatsApp send failed", e);
            return NodeResult.error(errorBatch(e.getMessage()));
        }
    }

    private NodeResult skipped(String reason) {
        return NodeResult.skip(
                new MessageBatch(
                        List.of(
                                new WorkflowMessage(
                                        Map.of(
                                                "status", "skipped",
                                                "reason", reason,
                                                "provider", "twilio"
                                        )
                                )
                        )
                )
        );
    }

    private NodeResult success(String to, String from) {
        return NodeResult.complected(
                "success",
                new MessageBatch(
                        List.of(
                                new WorkflowMessage(
                                        Map.of(
                                                "status", "sent",
                                                "to", to,
                                                "from", from,
                                                "provider", "twilio"
                                        )
                                )
                        )
                )
        );
    }

    private String extractBody(Map<String, Object> resJson) {
        if (resJson == null) return "";
        Object message = resJson.get("message");
        if (message instanceof String) {
            return (String) message;
        }
        Object responseObj = resJson.get("response");
        if (responseObj instanceof Map) {
            Map<?, ?> response = (Map<?, ?>) responseObj;
            Object kwargsObj = response.get("kwargs");
            if (kwargsObj instanceof Map) {
                Map<?, ?> kwargs = (Map<?, ?>) kwargsObj;
                Object content = kwargs.get("content");
                return content != null ? String.valueOf(content) : "";
            }
        }
        return "";
    }
    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private boolean isValidPhone(String value) {
        return value != null && value.matches("^\\+[1-9]\\d{7,14}$");
    }

    private MessageBatch errorBatch(String message) {
        return new MessageBatch(
                List.of(
                        new WorkflowMessage(
                                Map.of("error", message)
                        )
                )
        );
    }
}
