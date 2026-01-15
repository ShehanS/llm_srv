package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.ResponseCode;
import com.shehan.llmsvr.dtos.ResponseMessage;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.RoutingEntity;
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

    @GetMapping("/full")
    public Mono<ResponseEntity<ResponseMessage>> getFullConfig() {
        return configService.getFullConfig()
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


    @PostMapping("/tools")
    public Mono<ResponseEntity<ResponseMessage>> addTool(@RequestBody ToolEntity tool) {
        return configService.addTool(tool)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool saved successfully", res, null)))
                .onErrorResume(this::handleError);
    }


    @PostMapping("/link/{agentId}/{toolId}")
    public Mono<ResponseEntity<ResponseMessage>> linkTool(@PathVariable Integer agentId, @PathVariable Integer toolId) {
        return configService.linkToolToAgent(agentId, toolId)
                .then(Mono.just(ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Tool linked to agent successfully", null, null))))
                .onErrorResume(this::handleError);
    }


    @PutMapping("/routing")
    public Mono<ResponseEntity<ResponseMessage>> updateRouting(@RequestBody RoutingEntity routing) {
        return configService.updateRoutingConfig(routing)
                .map(res -> ResponseEntity.ok(
                        new ResponseMessage(ResponseCode.SUCCESS.getCode(), "Routing updated successfully", res, null)))
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
