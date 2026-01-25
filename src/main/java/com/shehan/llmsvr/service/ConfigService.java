package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingConfig;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.entites.AgentEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConfigService {

    Mono<MainConfig> getFullConfig(String routeName);

    Mono<RoutingConfig> getRouteConfig(String routeName);

    Mono<Agent> addAgent(AgentEntity agent);

    Mono<Agent> updateAgent(Integer id, Agent agent);

    Flux<Agent> getAllAgents();

    Mono<Void> deleteAgent(Integer id);

    Flux<Tool> getAllTools();

    Mono<Void> linkToolToAgent(Integer agentId, String toolName);

    Mono<Void> unlinkToolFromAgent(Integer agentId, String toolName);

    Flux<RoutingConfig> getRoutingConfigs();

    Mono<RoutingConfig> addRoutingConfigs(RoutingConfig routingConfig);

    Mono<RoutingConfig> updateRoutingConfig(RoutingConfig routing);

    Mono<Void> deleteRouteConfig(Integer id);

    Mono<Void> linkAgentToRoute(Integer routeId, Integer agentId);

    Mono<Void> unlinkAgentFromRoute(Integer routeId, Integer agentId);

    Mono<Void> linkAgentToRouteByName(String routeName, String agentName);

    Mono<Void> unlinkAgentFromRouteByName(String routeName, String agentName);
}
