package com.shehan.llmsvr.config;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.service.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTraceWebSocketHandler implements WebSocketHandler {

    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String runId = extractRunId(session);

        if (runId == null || runId.isBlank()) {
            return session.close();
        }
        Flux<WebSocketMessage> traceStream =
                workflowEngine.liveTrace(runId)
                        .map(t -> toMessage(session, t));

        // Sends a ping every 20 seconds to prevent the browser/proxy from closing the idle socket
        Flux<WebSocketMessage> keepAlive =
                Flux.interval(Duration.ofSeconds(20))
                        .map(i -> session.pingMessage(db -> db.wrap(new byte[0])));

        // Merge both. This will run forever until the frontend disconnects.
        return session.send(Flux.merge(traceStream, keepAlive))
                .doOnError(e -> log.error("WS Error for runId {}: {}", runId, e.getMessage()))
                .doFinally(signal -> log.info("WS Connection closed for runId: {}", runId));
    }

    private WebSocketMessage toMessage(WebSocketSession session, ExecutionTrace trace) {
        try {
            return session.textMessage(objectMapper.writeValueAsString(trace));
        } catch (Exception e) {
            return session.textMessage("{\"error\":\"serialization\"}");
        }
    }

    private String extractRunId(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}
