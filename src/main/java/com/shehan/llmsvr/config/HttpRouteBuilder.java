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
    // FIXED: Inject the Spring Reactive Engine wrapper instead of the Temporal Orchestrator Interface
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
                                .defaultIfEmpty(new HashMap<>())
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

                                    // FIXED: engine.runFromNode returns Mono<String>, compatible with reactive pipeline mapping operators
                                    return engine.runFromNode(batch, wf.getDefinition(), nodeId, flowId)
                                            .flatMap(runId -> ServerResponse.ok()
                                                    .contentType(MediaType.parseMediaType(mediaType))
                                                    .bodyValue(Map.of(
                                                            "success", true,
                                                            "flowId", flowId,
                                                            "runId", runId,
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
                                    String contact = form.getFirst("From") != null ? form.getFirst("From").replace("whatsapp:", "") : "";
                                    bodyData.put("contact", contact);
                                    bodyData.put("message", form.getFirst("Body"));

                                    Map<String, Object> data = new HashMap<>();
                                    data.put("provider", "whatsapp");
                                    data.put("method", "POST");
                                    data.put("body", bodyData);
                                    data.put("nodeId", nodeId);
                                    data.put("flowId", flowId);

                                    // FIXED: engine.runFromNode returns Mono<String>, chained into a reactive stream response properly
                                    return engine.runFromNode(
                                            new MessageBatch(List.of(new WorkflowMessage(data))),
                                            workflow.getDefinition(),
                                            nodeId,
                                            flowId
                                    ).flatMap(runId -> ServerResponse.ok()
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
        String flowId = request.pathVariable("flowId");
        String sessionId = request.pathVariable("sessionId");

        return request.bodyToMono(Map.class)
                .defaultIfEmpty(new HashMap<>())
                .flatMap(body -> {
                    Map<String, Object> approvalData = new HashMap<>(body);
                    approvalData.put("sessionId", sessionId);
                    MessageBatch humanInput = new MessageBatch(List.of(new WorkflowMessage(approvalData)));
                    String outputHandle = body.getOrDefault("action", "success").toString();

                    log.info("Resuming workflow instance: {} with action: {}", flowId, outputHandle);
                    if (flowId.isEmpty() || sessionId.isEmpty()) {
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "status", "error",
                                        "message", "cannot empty flowId or sessionId",
                                        "timestamp", System.currentTimeMillis()
                                ));
                    }

                    // FIXED: Changed engine method call from .runTemporalFromNode to .resume(flowId, humanInput, outputHandle)
                    return engine.resume(flowId, humanInput, outputHandle)
                            .flatMap(id -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of(
                                            "status", "resumed",
                                            "runId", id,
                                            "sessionId", sessionId,
                                            "timestamp", System.currentTimeMillis()
                                    )));
                })
                .onErrorResume(e -> {
                    log.error("Approval resume error for runId: {}", flowId, e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", e.getMessage()));
                });
    }
}