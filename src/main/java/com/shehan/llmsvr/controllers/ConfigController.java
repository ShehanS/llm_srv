package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.service.CommonToolService;
import com.shehan.llmsvr.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final CommonToolService commonToolService;

    @GetMapping("/full/{routeName}")
    public Mono<ResponseEntity<ResponseMessage>> getFullConfig(@PathVariable String routeName) {
        return configService.getFullConfig(routeName)
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Full configuration retrieved", res)))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/agents")
    public Mono<ResponseEntity<ResponseMessage>> addAgent(@RequestBody Agent agent) {
        return configService.addAgent(agent)
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Agent saved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/agents/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> deleteAgent(@PathVariable Integer agentId) {
        return configService.deleteAgent(agentId)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Agent deleted successfully", null))))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/agents/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> updateAgent(@PathVariable Integer agentId, @RequestBody Agent agent) {
        return configService.updateAgent(agentId, agent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Agent updated successfully", null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/link/{agentId}/{toolName}/route-agent/{routeAgent}")
    public Mono<ResponseEntity<ResponseMessage>> linkTool(@PathVariable Integer agentId, @PathVariable String toolName, @PathVariable String routeAgent) {
        return configService.linkToolToAgent(agentId, toolName, routeAgent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("AgentTool linked successfully", null))))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/unlink/{agentId}/{toolName}/route-agent/{routeAgent}")
    public Mono<ResponseEntity<ResponseMessage>> unLinkTool(@PathVariable Integer agentId, @PathVariable String toolName, @PathVariable String routeAgent) {
        return configService.unlinkToolFromAgent(agentId, toolName, routeAgent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("AgentTool unlinked successfully", null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/routing-agents/{routeId}/link-agent/{agentId}/route-agent/{routeAgent}")
    public Mono<ResponseEntity<ResponseMessage>> linkAgentToRouteAgent(@PathVariable Integer routeId, @PathVariable Integer agentId, @PathVariable String routeAgent) {
        return configService.linkAgentToRouteAgent(routeId, agentId, routeAgent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Agent linked to route successfully", null))))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/routing-agents/{routeId}/unlink-agent/{agentId}/route-agent/{routeAgent}")
    public Mono<ResponseEntity<ResponseMessage>> unlinkAgentFromRouteAgent(@PathVariable Integer routeId, @PathVariable Integer agentId, @PathVariable String routeAgent) {
        return configService.unlinkAgentFromRouteAgent(routeId, agentId, routeAgent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Agent unlinked from route successfully", null))))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/agents")
    public Mono<ResponseEntity<ResponseMessage>> getAllAgents() {
        return configService.getAllAgents().collectList()
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Agents retrieved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/common-tools")
    public Mono<ResponseEntity<ResponseMessage>> getCommonTools() {
        return commonToolService.loadAllCommonTools().collectList()
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Common Tools retrieved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/tools")
    public Mono<ResponseEntity<ResponseMessage>> getAllTools() {
        return configService.getAllTools().collectList()
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Tools retrieved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/tools/{id}")
    public Mono<ResponseEntity<ResponseMessage>> deleteTool(@PathVariable Integer id) {
        return configService.deleteTool(id)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Tool deleted successfully", null))))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/tools/{id}")
    public Mono<ResponseEntity<ResponseMessage>> updateTool(@PathVariable Integer id, @RequestBody AgentTool tool) {
        return configService.updateTool(id, tool)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Tool deleted successfully", null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/tools/copy")
    public Mono<ResponseEntity<ResponseMessage>> copyTool(@RequestBody AgentTool tool) {
        return configService.copyTool(tool)
                .map(res -> ResponseEntity.ok(
                        buildSuccessResponse("Tool copy operation completed", res)
                ))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/tools/{toolName}/mark-dangerous/{routeAgent}")
    public Mono<ResponseEntity<ResponseMessage>> markDangerousTool(
            @PathVariable String toolName,
            @RequestParam Boolean dangerous, @PathVariable String routeAgent) {
        return configService.markDangerousTool(toolName, dangerous, routeAgent)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("AgentTool danger status updated successfully", null))))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/routing-agents/all")
    public Mono<ResponseEntity<ResponseMessage>> getRoutingConfigs() {
        return configService.getRoutingConfigs().collectList()
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Routing configs retrieved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/routing-agents/{routeName}")
    public Mono<ResponseEntity<ResponseMessage>> getRoutingConfig(@PathVariable String routeName) {
        return configService.getRouteConfig(routeName)
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Routing config retrieved successfully", res)))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/routing-agents")
    public Mono<ResponseEntity<ResponseMessage>> addRoutingAgent(@RequestBody RoutingAgent routingAgent) {
        return configService.addRoutingAgent(routingAgent)
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Routing config added successfully", res)))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/routing-agents")
    public Mono<ResponseEntity<ResponseMessage>> updateRoutingAgent(@RequestBody RoutingAgent routingAgent) {
        return configService.updateRoutingAgent(routingAgent)
                .map(res -> ResponseEntity.ok(buildSuccessResponse("Routing config updated successfully", res)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/routing-agents/{id}")
    public Mono<ResponseEntity<ResponseMessage>> deleteRouteAgent(@PathVariable Integer id) {
        return configService.deleteRouteAgent(id)
                .then(Mono.just(ResponseEntity.ok(buildSuccessResponse("Route config deleted successfully", null))))
                .onErrorResume(this::handleError);
    }


    private ResponseMessage buildSuccessResponse(String message, Object data) {
        return ResponseMessage.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    private Mono<ResponseEntity<ResponseMessage>> handleError(Throwable ex) {
        log.error("API Error: ", ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseMessage.builder()
                                .code(ResponseCode.ERROR.getCode())
                                .message("Operation failed")
                                .error(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
        );
    }
}
