package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.RoutingEntity;
import com.shehan.llmsvr.entites.ToolEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConfigService {

    Mono<MainConfig> getFullConfig();

    Mono<Agent> addAgent(AgentEntity agent);

    Mono<Agent> updateAgent(Integer id, AgentEntity agent);

    Flux<Agent> getAllAgents();

    Mono<Void> deleteAgent(Integer id);

    Mono<Tool> addTool(ToolEntity tool);

    Mono<Tool> updateTool(Integer id, ToolEntity tool);

    Flux<Tool> getAllTools();

    Mono<Void> deleteTool(Integer id);

    Mono<Void> linkToolToAgent(Integer agentId, Integer toolId);

    Mono<Void> unlinkToolFromAgent(Integer agentId, Integer toolId);

    Mono<RoutingEntity> getRoutingConfig();

    Mono<RoutingEntity> updateRoutingConfig(RoutingEntity routing);
}
