package com.shehan.llmsvr.config;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpRouteBuilder {

    private final WorkflowService workflowService;
    private final WorkflowEngine engine;

    public Mono<RouterFunction<ServerResponse>> buildAsync() {

        return workflowService.getAll()
                .collectList()
                .map(workflows -> {

                    List<RouterFunction<ServerResponse>> routes = new ArrayList<>();

                    for (Workflow workflow : workflows) {
                        workflow.getDefinition().getNodes().forEach(node -> {

                            boolean isHttpTrigger = "trigger.http".equals(node.getType());
                            boolean isWhatsappTrigger = "trigger.whatsapp".equals(node.getType());
                            if (!isHttpTrigger && !isWhatsappTrigger) return;

                            Map<String, Object> config = node.getConfig();

                            String methodCfg = NodeConfigUtil.getInputProp(
                                    config, "method", "POST"
                            );

                            String path = NodeConfigUtil.getInputProp(
                                    config,
                                    "path",
                                    isWhatsappTrigger
                                            ? "/webhook/whatsapp/{flowId}"
                                            : "/webhook/http/{flowId}"
                            );

                            String mediaType = NodeConfigUtil.getInputProp(
                                    config, "mediaType", "application/json"
                            );

                            String verifyToken = NodeConfigUtil.getInputProp(
                                    config, "verifyToken", null
                            );

                            Map<String, String> cfg = new HashMap<>();
                            cfg.put("method", methodCfg);
                            cfg.put("path", path);
                            cfg.put("mediaType", mediaType);
                            if (verifyToken != null) {
                                cfg.put("verifyToken", verifyToken);
                            }

                            RequestPredicate methodPredicate;

                            if (methodCfg.contains("/") || methodCfg.contains(",")) {
                                String[] methods = methodCfg.split("[/,]");
                                methodPredicate = Arrays.stream(methods)
                                        .map(String::trim)
                                        .map(String::toUpperCase)
                                        .map(HttpMethod::valueOf)
                                        .map(RequestPredicates::method)
                                        .reduce(RequestPredicate::or)
                                        .orElseThrow();
                            } else {
                                methodPredicate = RequestPredicates.method(
                                        HttpMethod.valueOf(methodCfg.toUpperCase())
                                );
                            }

                            RequestPredicate predicate =
                                    RequestPredicates.path(path)
                                            .and(methodPredicate);

                            routes.add(
                                    RouterFunctions.route(
                                            predicate,
                                            req -> isWhatsappTrigger
                                                    ? handleWhatsappRequest(req, cfg)
                                                    : handleHTTPRequest(req, cfg)
                                    )
                            );
                        });
                    }

                    if (routes.isEmpty()) {
                        return RouterFunctions.route(
                                RequestPredicates.GET("/__noop"),
                                req -> ServerResponse.notFound().build()
                        );
                    }

                    return routes.stream()
                            .reduce(RouterFunction::and)
                            .orElseThrow();
                });
    }

    private Mono<ServerResponse> handleHTTPRequest(
            ServerRequest request,
            Map<String, String> config
    ) {

        String mediaType = config.getOrDefault("mediaType", "application/json");
        Map<String, String> pathVars = request.pathVariables();
        String flowId = pathVars.get("flowId");

        return workflowService.open(flowId)
                .flatMap(wf ->
                        request.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .flatMap(body -> {

                                    Map<String, Object> messageData = Map.of(
                                            "provider", "http",
                                            "method", request.method().name(),
                                            "path", request.path(),
                                            "pathVariables", pathVars,
                                            "query", request.queryParams().toSingleValueMap(),
                                            "headers", request.headers().asHttpHeaders().toSingleValueMap(),
                                            "body", body
                                    );

                                    MessageBatch batch =
                                            new MessageBatch(List.of(new WorkflowMessage(messageData)));

                                    return engine.run(batch, wf.getDefinition())
                                            .then(ServerResponse.ok()
                                                    .contentType(MediaType.parseMediaType(mediaType))
                                                    .bodyValue(Map.of(
                                                            "success", true,
                                                            "flowId", flowId,
                                                            "timestamp", System.currentTimeMillis()
                                                    )));
                                })
                )
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(e -> {
                    log.error("HTTP trigger error", e);
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "success", false,
                                    "error", e.getMessage()
                            ));
                });
    }

    private Mono<ServerResponse> handleWhatsappRequest(
            ServerRequest request,
            Map<String, String> cfg
    ) {

        String verifyToken = cfg.get("verifyToken");
        String mediaType = cfg.getOrDefault("mediaType", "application/json");

        if (request.method() == HttpMethod.GET) {

            String mode = request.queryParam("hub.mode").orElse(null);
            String token = request.queryParam("hub.verify_token").orElse(null);
            String challenge = request.queryParam("hub.challenge").orElse(null);

            if ("subscribe".equals(mode)
                    && verifyToken != null
                    && verifyToken.equals(token)) {

                log.info("WhatsApp webhook verified");
                return ServerResponse.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue(challenge);
            }

            return ServerResponse.status(HttpStatus.FORBIDDEN)
                    .bodyValue("Verification failed");
        }
        Map<String, String> pathVars = request.pathVariables();
        String flowId = pathVars.get("flowId");

        return workflowService.open(flowId)
                .switchIfEmpty(Mono.error(new RuntimeException("Workflow not found")))
                .flatMap(wf ->
                        request.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .flatMap(body -> {

                                    Map<String, Object> messageData = Map.of(
                                            "provider", "whatsapp",
                                            "method", request.method().name(),
                                            "path", request.path(),
                                            "query", request.queryParams().toSingleValueMap(),
                                            "headers", request.headers().asHttpHeaders().toSingleValueMap(),
                                            "body", body
                                    );

                                    MessageBatch batch =
                                            new MessageBatch(List.of(new WorkflowMessage(messageData)));

                                    return engine.run(batch, wf.getDefinition())
                                            .then(ServerResponse.ok()
                                                    .contentType(MediaType.parseMediaType(mediaType))
                                                    .bodyValue(Map.of(
                                                            "success", true,
                                                            "provider", "whatsapp",
                                                            "flowId", flowId,
                                                            "timestamp", System.currentTimeMillis()
                                                    )));
                                })
                )
                .onErrorResume(e -> {
                    log.error("WhatsApp trigger error", e);
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "success", false,
                                    "error", e.getMessage()
                            ));
                });
    }
}
