package com.shehan.llmsvr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.service.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTraceWebSocketHandler implements WebSocketHandler {

    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;
    private final UriTemplate pathTemplate = new UriTemplate("/api/workflow/ws/trace/{runId}");

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String runId = extractRunId(session);

        if (runId == null || runId.isBlank()) {
            log.warn("WebSocket handshake rejected: Run ID could not be parsed from path.");
            return session.close();
        }

        log.info("WebSocket connection open. Streaming live execution trace updates for runId: {}", runId);

        Flux<WebSocketMessage> traceStream =
                workflowEngine.liveTrace(runId)
                        .map(t -> toMessage(session, t));

        Flux<WebSocketMessage> keepAlive =
                Flux.interval(Duration.ofSeconds(20))
                        .map(i -> session.pingMessage(factory -> factory.wrap(new byte[0])));

        return session.send(Flux.merge(traceStream, keepAlive))
                .doOnError(e -> log.error("WebSocket Exception for runId {}: {}", runId, e.getMessage()))
                .doFinally(signalType -> log.info("WebSocket session terminated for runId: {} with signal: {}", runId, signalType));
    }

    private WebSocketMessage toMessage(WebSocketSession session, ExecutionTrace trace) {
        try {
            return session.textMessage(objectMapper.writeValueAsString(trace));
        } catch (Exception e) {
            log.error("Trace message JSON serialization failed", e);
            return session.textMessage("{\"error\":\"serialization\"}");
        }
    }

    private String extractRunId(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String prefix = "/api/workflow/ws/trace/";
        return path.contains(prefix) ? path.substring(path.indexOf(prefix) + prefix.length()) : null;
    }
}