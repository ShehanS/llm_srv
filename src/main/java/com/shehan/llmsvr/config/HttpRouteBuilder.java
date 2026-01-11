package com.shehan.llmsvr.config;

import com.shehan.llmsvr.dtos.FlowNode;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builds HTTP routes dynamically from workflow definitions
 */
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
                            if (!"trigger.http".equals(node.getType())) return;
                            Map<String, String> cfg = extractConfig(node);
                            String methodCfg = cfg.getOrDefault("method", "POST");

                            RequestPredicate methodPredicate;

                            if (methodCfg.contains("/") || methodCfg.contains(",")) {
                                String[] methods = methodCfg.split("[/,]");
                                methodPredicate = Arrays.stream(methods)
                                        .map(String::trim)
                                        .map(HttpMethod::valueOf)
                                        .map(RequestPredicates::method)
                                        .reduce(RequestPredicate::or)
                                        .orElseThrow();

                            } else {
                                methodPredicate = RequestPredicates.method(HttpMethod.valueOf(methodCfg));
                            }

                            RequestPredicate predicate =
                                    RequestPredicates.path(cfg.get("path"))
                                            .and(methodPredicate);

                            routes.add(
                                    RouterFunctions.route(
                                            predicate,
                                            req -> handleRequest(req, cfg)
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

                    return routes.stream().reduce(RouterFunction::and).orElseThrow();
                });
    }

    private Map<String, String> extractConfig(FlowNode node) {

        String method = null;
        String path = null;
        String mediaType = null;

        List<Map<String, Object>> inputProps =
                (List<Map<String, Object>>) node.getConfig().get("inputProps");

        if (inputProps != null) {
            for (Map<String, Object> prop : inputProps) {

                String name = String.valueOf(prop.get("name"));
                String resolved = resolveValue(prop);

                if ("method".equals(name)) {
                    method = resolved;
                } else if ("path".equals(name)) {
                    path = resolved;
                } else if ("mediaType".equals(name)) {
                    mediaType = resolved;
                }
            }
        }

        if (method == null) method = "GET";
        if (path == null) path = "/default";
        if (mediaType == null) mediaType = "application/json";

        return Map.of(
                "method", method,
                "path", path,
                "mediaType", mediaType
        );
    }

    private Mono<ServerResponse> handleRequest(
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

                                    MessageBatch batch = new MessageBatch(
                                            List.of(new WorkflowMessage(messageData))
                                    );

                                    return engine.run(batch, wf.getDefinition())
                                            .then(
                                                    ServerResponse.ok()
                                                            .contentType(MediaType.parseMediaType(mediaType))
                                                            .bodyValue(Map.of(
                                                                    "success", true,
                                                                    "flowId", flowId,
                                                                    "method", request.method().name(),
                                                                    "path", request.path(),
                                                                    "timestamp", System.currentTimeMillis()
                                                            ))
                                            );
                                })
                )
                .switchIfEmpty(
                        ServerResponse.notFound().build()
                )
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

    private String resolveValue(Map<String, Object> prop) {

        Object valueObj = prop.get("value");

        if (valueObj instanceof Map<?, ?> map) {
            Object v = map.get("value");
            Object d = map.get("defaultValue");
            return v != null ? v.toString()
                    : d != null ? d.toString() : null;
        }

        return valueObj != null ? valueObj.toString() : null;
    }
}
