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
import org.springframework.util.LinkedMultiValueMap;
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
                            boolean isWhatsappTrigger = "whatsapp.receive".equals(node.getType());
                            boolean isHumanApprovalTrigger = "human.approval".equals(node.getType());

                            if (!isHttpTrigger && !isWhatsappTrigger && !isHumanApprovalTrigger) return;

                            Map<String, Object> config = node.getConfig();
                            String methodCfg = NodeConfigUtil.getInputProp(config, "method", "POST");
                            String path = NodeConfigUtil.getInputProp(config, "path",
                                    NodeConfigUtil.getInputProp(config, "inboundWebhookUrl", ""));
                            String mediaType = NodeConfigUtil.getInputProp(config, "mediaType", "application/json");
                            String verifyToken = NodeConfigUtil.getInputProp(config, "verifyToken", null);

                            Map<String, String> cfg = new HashMap<>();
                            cfg.put("method", methodCfg);
                            cfg.put("path", path);
                            cfg.put("mediaType", mediaType);
                            cfg.put("nodeId", node.getId());
                            cfg.put("workflowId", workflow.getFlowId());
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

                            RequestPredicate predicate = RequestPredicates.path(path).and(methodPredicate);

                            routes.add(
                                    RouterFunctions.route(
                                            predicate,
                                            req -> {
                                                if (isWhatsappTrigger) {
                                                    return handleWhatsappRequest(req, cfg);
                                                } else if (isHumanApprovalTrigger) {
                                                    return handleHumanApprovalRequest(req, cfg);
                                                } else {
                                                    return handleHTTPRequest(req, cfg);
                                                }
                                            }
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
                                    body.put("flowId", flowId);
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
                                    String nodeId = wf.getDefinition()
                                            .getNodes()
                                            .stream()
                                            .filter(n -> "trigger.http".equals(n.getType()))
                                            .findFirst().get().getId();

                                    return engine.runFromNode(batch, wf.getDefinition(), nodeId, flowId)
                                            .then(ServerResponse.ok()
                                                    .contentType(MediaType.parseMediaType(mediaType))
                                                    .bodyValue(Map.of(
                                                            "success", true,
                                                            "flowId", flowId,
                                                            "timestamp", System.currentTimeMillis()
                                                    )));
                                })
                )
                .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "success", false,
                                "message", "Path is not found",
                                "requestedFlowId", flowId != null ? flowId : "none"
                        )))
                .onErrorResume(Throwable.class, e -> {
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
            Map<String, String> config
    ) {
        String flowId = request.pathVariable("flowId");
        String nodeId = config.get("nodeId");
        String workflowId = config.get("workflowId");

        log.debug("Processing WhatsApp request for flowId: {}, nodeId: {}, workflowId: {}",
                flowId, nodeId, workflowId);

        return workflowService.open(flowId)
                .flatMap(workflow ->
                        request.formData()
                                .defaultIfEmpty(new LinkedMultiValueMap<>())
                                .flatMap(form -> {
                                    Map<String, Object> bodyData = new HashMap<>();
                                    String contact = form.getFirst("From").replace("whatsapp:", "");
                                    bodyData.put("contact", contact);
                                    bodyData.put("message", form.getFirst("Body"));

                                    Map<String, Object> data = new HashMap<>();
                                    data.put("provider", "whatsapp");
                                    data.put("method", "POST");
                                    data.put("body", bodyData);
                                    data.put("nodeId", nodeId);
                                    data.put("flowId", flowId);

                                    return engine.runFromNode(
                                            new MessageBatch(List.of(new WorkflowMessage(data))),
                                            workflow.getDefinition(),
                                            nodeId,
                                            flowId
                                    ).then(ServerResponse.ok()
                                            .contentType(MediaType.APPLICATION_XML)
                                            .bodyValue("<Response><Message>Thank you..., Your message is processing</Message></Response>"));
                                })
                )
                .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "success", false,
                                "message", "Workflow not found",
                                "requestedFlowId", flowId
                        )))
                .onErrorResume(e -> {
                    log.error("WhatsApp trigger error for flowId: {}", flowId, e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_XML)
                            .bodyValue("<Response><Message>Error processing request</Message></Response>");
                });
    }

    private Mono<ServerResponse> handleHumanApprovalRequest(
            ServerRequest request,
            Map<String, String> config
    ) {
        String runId = request.pathVariable("flowId");

        return request.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .flatMap(body -> {
                    Map<String, Object> approvalData = new HashMap<>(body);
                    MessageBatch humanInput = new MessageBatch(List.of(new WorkflowMessage(approvalData)));
                    String outputHandle = body.getOrDefault("action", "success").toString();

                    log.info("Resuming workflow instance: {} with action: {}", runId, outputHandle);

                    return engine.resume(runId, humanInput, outputHandle)
                            .flatMap(id -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of(
                                            "status", "resumed",
                                            "runId", id,
                                            "timestamp", System.currentTimeMillis()
                                    )));
                })
                .onErrorResume(e -> {
                    log.error("Approval resume error for runId: {}", runId, e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", e.getMessage()));
                });
    }
}
//
//
//
//
//package com.shehan.llmsvr.config;
//
//        import com.shehan.llmsvr.dtos.MessageBatch;
//        import com.shehan.llmsvr.dtos.Workflow;
//        import com.shehan.llmsvr.dtos.WorkflowMessage;
//        import com.shehan.llmsvr.helper.NodeConfigUtil;
//        import com.shehan.llmsvr.service.WorkflowEngine;
//        import com.shehan.llmsvr.service.WorkflowService;
//        import lombok.RequiredArgsConstructor;
//        import lombok.extern.slf4j.Slf4j;
//        import org.springframework.http.HttpMethod;
//        import org.springframework.http.HttpStatus;
//        import org.springframework.http.MediaType;
//        import org.springframework.stereotype.Component;
//        import org.springframework.util.LinkedMultiValueMap;
//        import org.springframework.web.reactive.function.server.*;
//        import reactor.core.publisher.Mono;
//
//        import java.util.*;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class HttpRouteBuilder {
//
//    private final WorkflowService workflowService;
//    private final WorkflowEngine engine;
//
//    public Mono<RouterFunction<ServerResponse>> buildAsync() {
//        return workflowService.getAll()
//                .collectList()
//                .map(workflows -> {
//                    RouterFunctions.Builder builder = RouterFunctions.route();
//
//                    Map<String, List<RouteConfig>> routesByType = new HashMap<>();
//                    routesByType.put("whatsapp.receive", new ArrayList<>());
//                    routesByType.put("human.approval", new ArrayList<>());
//                    routesByType.put("trigger.http", new ArrayList<>());
//
//                    for (Workflow workflow : workflows) {
//                        workflow.getDefinition().getNodes().forEach(node -> {
//                            final String nodeType = node.getType();
//                            final String nodeId = node.getId();
//
//                            if (!isTriggerNode(nodeType)) return;
//
//                            Map<String, Object> config = node.getConfig();
//                            String methodCfg = NodeConfigUtil.getInputProp(config, "method", "POST");
//                            String path = NodeConfigUtil.getInputProp(config, "path",
//                                    NodeConfigUtil.getInputProp(config, "inboundWebhookUrl", ""));
//
//                            if (path.isEmpty()) return;
//                            path = extractPath(path);
//                            routesByType.get(nodeType).add(new RouteConfig(nodeType, nodeId, path, methodCfg, workflow));
//                        });
//                    }
//                    registerRoutes(builder, routesByType.get("whatsapp.receive"));
//                    registerRoutes(builder, routesByType.get("human.approval"));
//                    registerRoutes(builder, routesByType.get("trigger.http"));
//
//                    RouterFunction<ServerResponse> fallbackRoute = RouterFunctions.route(
//                            RequestPredicates.all()
//                                    .and(RequestPredicates.path("/api/**").negate())
//                                    .and(RequestPredicates.path("/workflow/ws/**").negate()),
//                            req -> {
//                                log.warn("No route matched for path: {} (method: {})",
//                                        req.path(), req.method());
//                                return ServerResponse.status(HttpStatus.NOT_FOUND)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .bodyValue(Map.of(
//                                                "success", false,
//                                                "message", "Path not found",
//                                                "path", req.path()
//                                        ));
//                            }
//                    );
//
//                    return builder.build().and(fallbackRoute);
//                });
//    }
//
//    private String extractPath(String fullPath) {
//        String path = fullPath;
//        if (fullPath.startsWith("http://") || fullPath.startsWith("https://")) {
//            try {
//                java.net.URL url = new java.net.URL(fullPath);
//                path = url.getPath();
//            } catch (Exception e) {
//                log.warn("Failed to parse URL: {}", fullPath);
//            }
//        }
//        if (path.startsWith("/service/")) {
//            path = path.substring("/service".length());
//            log.debug("Stripped context path '/service' from route: {} -> {}", fullPath, path);
//        }
//
//        return path;
//    }
//
//    private void registerRoutes(RouterFunctions.Builder builder, List<RouteConfig> routes) {
//        routes.sort((r1, r2) -> {
//            int segments1 = r1.path.split("/").length;
//            int segments2 = r2.path.split("/").length;
//            if (segments1 != segments2) {
//                return Integer.compare(segments2, segments1);
//            }
//            int vars1 = countPathVariables(r1.path);
//            int vars2 = countPathVariables(r2.path);
//            if (vars1 != vars2) {
//                return Integer.compare(vars1, vars2);
//            }
//            return Integer.compare(r2.path.length(), r1.path.length());
//        });
//
//        for (RouteConfig route : routes) {
//            log.info("Registering {} route: {} -> {}", route.nodeType, route.path, route.nodeId);
//
//            RequestPredicate predicate = RequestPredicates.path(route.path)
//                    .and(buildMethodPredicate(route.methodCfg));
//
//            builder.add(RouterFunctions.route(predicate, request -> {
//                log.info("Matched {} route: {} (nodeId: {})",
//                        route.nodeType, request.path(), route.nodeId);
//
//                if ("whatsapp.receive".equals(route.nodeType)) {
//                    return handleWhatsappRequest(request, route.nodeId, route.workflow);
//                } else if ("human.approval".equals(route.nodeType)) {
//                    return handleHumanApprovalRequest(request, route.nodeId, route.workflow);
//                } else {
//                    return handleHTTPRequest(request, route.nodeId, route.workflow);
//                }
//            }));
//        }
//    }
//
//    private int countPathVariables(String path) {
//        return (int) path.chars().filter(ch -> ch == '{').count();
//    }
//
//    private boolean isTriggerNode(String type) {
//        return "trigger.http".equals(type) || "whatsapp.receive".equals(type) || "human.approval".equals(type);
//    }
//
//    private RequestPredicate buildMethodPredicate(String methodCfg) {
//        return Arrays.stream(methodCfg.split("[/,]"))
//                .map(m -> HttpMethod.valueOf(m.trim().toUpperCase()))
//                .map(RequestPredicates::method)
//                .reduce(RequestPredicate::or)
//                .orElse(RequestPredicates.method(HttpMethod.POST));
//    }
//
//    @SuppressWarnings("unchecked")
//    private Mono<ServerResponse> handleHTTPRequest(ServerRequest request, String nodeId, Workflow workflow) {
//        String flowId = request.pathVariable("flowId");
//        log.debug("Processing HTTP request for flowId: {}, nodeId: {}", flowId, nodeId);
//
//        return request.bodyToMono(Map.class)
//                .map(body -> new HashMap<String, Object>((Map<String, Object>) body))
//                .defaultIfEmpty(new HashMap<>())
//                .flatMap(body -> {
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("provider", "http");
//                    data.put("method", request.method().name());
//                    data.put("body", body);
//                    data.put("nodeId", nodeId);
//                    return engine.runFromNode(
//                            new MessageBatch(List.of(new WorkflowMessage(data))),
//                            workflow.getDefinition(),
//                            nodeId,
//                            flowId
//                    ).then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
//                            .bodyValue(Map.of("success", true, "flowId", flowId)));
//                });
//    }
//
//    private Mono<ServerResponse> handleWhatsappRequest(ServerRequest request, String nodeId, Workflow workflow) {
//        String flowId = request.pathVariable("flowId");
//        log.debug("Processing WhatsApp request for flowId: {}, nodeId: {}", flowId, nodeId);
//
//        return request.formData()
//                .defaultIfEmpty(new LinkedMultiValueMap<>())
//                .flatMap(form -> {
//                    Map<String, Object> bodyData = new HashMap<>();
//                    bodyData.put("contact", form.getFirst("From"));
//                    bodyData.put("message", form.getFirst("Body"));
//
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("provider", "whatsapp");
//                    data.put("method", "POST");
//                    data.put("body", bodyData);
//                    data.put("nodeId", nodeId);
//                    return engine.runFromNode(
//                            new MessageBatch(List.of(new WorkflowMessage(data))),
//                            workflow.getDefinition(),
//                            nodeId,
//                            flowId
//                    ).then(ServerResponse.ok().contentType(MediaType.APPLICATION_XML)
//                            .bodyValue("<Response><Message>Processed</Message></Response>"));
//                });
//    }
//
//    @SuppressWarnings("unchecked")
//    private Mono<ServerResponse> handleHumanApprovalRequest(ServerRequest request, String nodeId, Workflow workflow) {
//        String runId = request.pathVariable("flowId");
//        log.debug("Processing Human Approval request for runId: {}, nodeId: {}", runId, nodeId);
//
//        return request.bodyToMono(Map.class)
//                .map(body -> new HashMap<String, Object>((Map<String, Object>) body))
//                .defaultIfEmpty(new HashMap<>())
//                .flatMap(body -> engine.resume(runId, new MessageBatch(List.of(new WorkflowMessage(body))),
//                                String.valueOf(body.getOrDefault("action", "success")))
//                        .flatMap(id -> ServerResponse.ok().bodyValue(Map.of("status", "resumed", "runId", id))));
//    }
//
//    private static class RouteConfig {
//        final String nodeType;
//        final String nodeId;
//        final String path;
//        final String methodCfg;
//        final Workflow workflow;
//
//        RouteConfig(String nodeType, String nodeId, String path, String methodCfg, Workflow workflow) {
//            this.nodeType = nodeType;
//            this.nodeId = nodeId;
//            this.path = path;
//            this.methodCfg = methodCfg;
//            this.workflow = workflow;
//        }
//    }
//}
