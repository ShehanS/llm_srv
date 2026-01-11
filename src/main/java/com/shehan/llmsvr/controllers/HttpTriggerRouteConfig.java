//package com.shehan.llmsvr.controllers;
//
//import com.shehan.llmsvr.dtos.Workflow;
//import com.shehan.llmsvr.service.WorkflowService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.server.*;
//import reactor.core.publisher.Mono;
//
//import java.util.*;
//
///**
// * Dynamic HTTP trigger routing for workflows
// */
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class HttpTriggerRouteConfig {
//
//    private final WorkflowService workflowService;
//
//    @Bean
//    public RouterFunction<ServerResponse> httpTriggerRoutes() {
//
//        List<Workflow> workflows = workflowService.getAll()
//                .collectList()
//                .block();
//
//        List<RouterFunction<ServerResponse>> routeList = new ArrayList<>();
//
//        if (workflows != null) {
//            for (Workflow workflow : workflows) {
//
//                workflow.getDefinition().getNodes().forEach(node -> {
//
//                    if (!"trigger.http".equals(node.getType())) {
//                        return;
//                    }
//
//                    List<Map<String, Object>> inputProps =
//                            (List<Map<String, Object>>) node.getConfig().get("inputProps");
//
//                    Map<String, Map<String, String>> routeConfigs =
//                            routeConfigure(inputProps);
//
//                    for (Map<String, String> config : routeConfigs.values()) {
//
//                        String path = config.get("path");
//                        String method = config.get("method");
//
//                        if (path == null || method == null) {
//                            log.warn("Skipping invalid route config: {}", config);
//                            continue;
//                        }
//
//                        log.info("Registering HTTP trigger: {} {}", method, path);
//
//                        RequestPredicate predicate = RequestPredicates
//                                .method(HttpMethod.valueOf(method))
//                                .and(RequestPredicates.path(path));
//
//                        routeList.add(
//                                RouterFunctions.route(
//                                        predicate,
//                                        request -> handleRequest(request, config)
//                                )
//                        );
//                    }
//                });
//            }
//        }
//
//        // ✅ IMPORTANT: Add fallback route if none registered
//        if (routeList.isEmpty()) {
//            log.warn("No HTTP trigger routes found. Registering fallback route.");
//
//            return RouterFunctions.route(
//                    RequestPredicates.all(),
//                    request -> ServerResponse.notFound().build()
//            );
//        }
//
//        // Combine all dynamic routes
//        return routeList.stream()
//                .reduce(RouterFunction::and)
//                .orElseThrow(); // safe because list is not empty
//    }
//
//
//    private Mono<ServerResponse> handleRequest(
//            ServerRequest request,
//            Map<String, String> config
//    ) {
//
//        String mediaType = config.getOrDefault("mediaType", "application/json");
//
//        Map<String, String> pathVars = request.pathVariables();
//
//        log.info("Handling {} {} | vars={}",
//                request.method(), request.path(), pathVars);
//
//        return request.bodyToMono(Map.class)
//                .defaultIfEmpty(new HashMap<>())
//                .flatMap(body -> {
//
//                    Map<String, Object> response = new LinkedHashMap<>();
//                    response.put("success", true);
//                    response.put("method", request.method().name());
//                    response.put("path", request.path());
//                    response.put("pathVariables", pathVars);
//                    response.put("queryParams", request.queryParams().toSingleValueMap());
//                    response.put("headers", request.headers().asHttpHeaders().toSingleValueMap());
//                    response.put("body", body);
//                    response.put("timestamp", System.currentTimeMillis());
//
//                    return ServerResponse.ok()
//                            .contentType(MediaType.parseMediaType(mediaType))
//                            .bodyValue(response);
//                })
//                .onErrorResume(e -> {
//                    log.error("HTTP trigger error", e);
//                    return ServerResponse.badRequest()
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .bodyValue(Map.of(
//                                    "success", false,
//                                    "error", e.getMessage()
//                            ));
//                });
//    }
//
//    private Map<String, Map<String, String>> routeConfigure(
//            List<Map<String, Object>> inputProps
//    ) {
//
//        String method = null;
//        String path = null;
//        String mediaType = null;
//
//        for (Map<String, Object> prop : inputProps) {
//
//            String name = String.valueOf(prop.get("name"));
//            Object valueObj = prop.get("value");
//            String resolved = null;
//
//            if (valueObj instanceof Map<?, ?> map) {
//                Object v = map.get("value");
//                Object d = map.get("defaultValue");
//                resolved = v != null ? v.toString()
//                        : d != null ? d.toString() : null;
//            } else if (valueObj instanceof String) {
//                resolved = valueObj.toString();
//            }
//
//            if ("method".equals(name)) {
//                method = resolved;
//            } else if ("path".equals(name)) {
//                path = resolved;
//            } else if ("mediaType".equals(name)) {
//                mediaType = resolved;
//            }
//        }
//
//        if (method == null) method = "POST";
//        if (path == null) path = "/default";
//        if (mediaType == null) mediaType = "application/json";
//
//        Map<String, Map<String, String>> routes = new HashMap<>();
//
//        routes.put(
//                UUID.randomUUID().toString(),
//                Map.of(
//                        "method", method,
//                        "path", path,
//                        "mediaType", mediaType
//                )
//        );
//
//        return routes;
//    }
//
//
//}
