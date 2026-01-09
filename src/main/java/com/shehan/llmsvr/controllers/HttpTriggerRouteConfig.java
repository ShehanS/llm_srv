package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.service.WorkflowService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Data
public class HttpTriggerRouteConfig {
    private final WorkflowService  workflowService;

    @Bean
    public RouterFunction<ServerResponse> httpTriggerRoutes() {

        workflowService.getAll()
                .collectList()
                .subscribe(workflows -> {
                    workflows.forEach(workflow -> {
                        log.info("Flow Name: {}", workflow.getDefinition());
                    });
                });

        Map<String, Map<String, String>> routeConfigs = new HashMap<>();

        routeConfigs.put("route1", Map.of(
                "path", "/test/{id}/{subId}",
                "method", "GET",
                "mediaType", "application/json"
        ));

        routeConfigs.put("route2", Map.of(
                "path", "/test/{id}/{subId}",
                "method", "POST",
                "mediaType", "application/json"
        ));

        routeConfigs.put("route3", Map.of(
                "path", "/api/users/{userId}",
                "method", "PUT",
                "mediaType", "application/json"
        ));

        RouterFunction<ServerResponse> routes = null;



        for (Map.Entry<String, Map<String, String>> entry : routeConfigs.entrySet()) {
            String routeName = entry.getKey();
            Map<String, String> config = entry.getValue();

            String path = config.get("path");
            String method = config.get("method");
            String mediaType = config.getOrDefault("mediaType", "application/json");

            log.info("Registering route: {} - {} {} ({})", routeName, method, path, mediaType);

            RequestPredicate predicate = RequestPredicates
                    .method(HttpMethod.valueOf(method))
                    .and(RequestPredicates.path(path));

            if (routes == null) {
                routes = RouterFunctions.route(predicate,
                        request -> handleRequest(request, config));
            } else {
                routes = routes.andRoute(predicate,
                        request -> handleRequest(request, config));
            }
        }

        return routes;
    }

    private Mono<ServerResponse> handleRequest(ServerRequest request, Map<String, String> config) {
        String mediaType = config.getOrDefault("mediaType", "application/json");

        // Extract all path variables
        Map<String, String> pathVars = request.pathVariables();

        log.info("Handling: {} {} | pathVars={}",
                request.method(), request.path(), pathVars);

        // Read body as Map
        return request.bodyToMono(Map.class)
                .defaultIfEmpty(new HashMap<>())
                .flatMap(bodyMap -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("path", request.path());
                    response.put("method", request.method().name());

                    // Path variables
                    response.put("pathVariables", pathVars);

                    // Specific path variables (if they exist)
                    if (pathVars.containsKey("id")) {
                        response.put("id", pathVars.get("id"));
                    }
                    if (pathVars.containsKey("subId")) {
                        response.put("subId", pathVars.get("subId"));
                    }
                    if (pathVars.containsKey("userId")) {
                        response.put("userId", pathVars.get("userId"));
                    }

                    // Query parameters
                    response.put("queryParams", request.queryParams().toSingleValueMap());

                    // Request body
                    response.put("body", bodyMap);

                    // Headers
                    response.put("headers", request.headers().asHttpHeaders().toSingleValueMap());

                    response.put("timestamp", System.currentTimeMillis());

                    return ServerResponse.ok()
                            .contentType(MediaType.parseMediaType(mediaType))
                            .bodyValue(response);
                })
                .onErrorResume(e -> {
                    log.error("Error handling request: {}", e.getMessage(), e);
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "success", false,
                                    "error", e.getMessage()
                            ));
                });
    }
}
