package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.ToolEntity;
import com.shehan.llmsvr.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping("/full/{routeName}")
    public Mono<ResponseEntity<ResponseMessage>> getFullConfig(@PathVariable String routeName) {
        return configService.getFullConfig(routeName)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Full configuration retrieved", res, null)))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/agents")
    public Mono<ResponseEntity<ResponseMessage>> addAgent(@RequestBody AgentEntity agent) {
        return configService.addAgent(agent)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agent saved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/agents/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> deleteAgent(@PathVariable Integer agentId) {
        return configService.deleteAgent(agentId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agent deleted successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/agents/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> updateAgent(@PathVariable Integer agentId, @RequestBody Agent agent) {
        return configService.updateAgent(agentId, agent)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agent updated successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/tools/{toolId}")
    public Mono<ResponseEntity<ResponseMessage>> updateTool(@PathVariable Integer toolId, @RequestBody Tool tool) {
        return configService.updateTool(toolId, tool)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool updated successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/tools")
    public Mono<ResponseEntity<ResponseMessage>> addTool(@RequestBody ToolEntity tool) {
        return configService.addTool(tool)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool saved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/tools/{toolId}")
    public Mono<ResponseEntity<ResponseMessage>> deleteTool(@PathVariable Integer toolId) {
        return configService.deleteTool(toolId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool deleted successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/link/{agentId}/{toolId}")
    public Mono<ResponseEntity<ResponseMessage>> linkTool(@PathVariable Integer agentId, @PathVariable Integer toolId) {
        return configService.linkToolToAgent(agentId, toolId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool linked successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/unlink/{agentId}/{toolId}")
    public Mono<ResponseEntity<ResponseMessage>> unLinkTool(@PathVariable Integer agentId, @PathVariable Integer toolId) {
        return configService.unlinkToolFromAgent(agentId, toolId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool unlinked successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/routing-config/{routeId}/link-agent/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> linkAgentToRoute(@PathVariable Integer routeId, @PathVariable Integer agentId) {
        return configService.linkAgentToRoute(routeId, agentId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agent linked to route successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/routing-config/{routeId}/unlink-agent/{agentId}")
    public Mono<ResponseEntity<ResponseMessage>> unlinkAgentFromRoute(@PathVariable Integer routeId, @PathVariable Integer agentId) {
        return configService.unlinkAgentFromRoute(routeId, agentId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agent unlinked from route successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/agents")
    public Mono<ResponseEntity<ResponseMessage>> getAllAgents() {
        return configService.getAllAgents().collectList()
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Agents retrieved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/tools")
    public Mono<ResponseEntity<ResponseMessage>> getAllTools() {
        return configService.getAllTools().collectList()
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tools retrieved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/routing-config/all")
    public Mono<ResponseEntity<ResponseMessage>> getRoutingConfigs() {
        return configService.getRoutingConfigs().collectList()
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Routing configs retrieved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @GetMapping("/routing-config/{routeName}")
    public Mono<ResponseEntity<ResponseMessage>> getRoutingConfig(@PathVariable String routeName) {
        return configService.getRouteConfig(routeName)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Routing config retrieved successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @PostMapping("/routing-config")
    public Mono<ResponseEntity<ResponseMessage>> addRoutingConfig(@RequestBody RoutingConfig routingConfig) {
        return configService.addRoutingConfigs(routingConfig)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Routing config added successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @PutMapping("/routing-config")
    public Mono<ResponseEntity<ResponseMessage>> updateRoutingConfig(@RequestBody RoutingConfig routingConfig) {
        return configService.updateRoutingConfig(routingConfig)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Routing config updated successfully", res, null)))
                .onErrorResume(this::handleError);
    }

    @DeleteMapping("/routing-config/{id}")
    public Mono<ResponseEntity<ResponseMessage>> deleteRouteConfig(@PathVariable Integer id) {
        return configService.deleteRouteConfig(id)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Route config deleted successfully", null, null))))
                .onErrorResume(this::handleError);
    }

    private Mono<ResponseEntity<ResponseMessage>> handleError(Throwable ex) {
        log.error("API Error: ", ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ResponseMessage(
                                ResponseCode.ERROR.getCode(),
                                null,
                                ex.getMessage(),
                                "Operation failed"
                        ))
        );
    }
}
