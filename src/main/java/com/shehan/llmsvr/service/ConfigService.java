package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingAgent;
import com.shehan.llmsvr.dtos.AgentTool;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConfigService {

    Mono<MainConfig> getFullConfig(String routeName);

    Mono<RoutingAgent> getRouteConfig(String routeName);

    Mono<Agent> addAgent(Agent agent);

    Mono<Agent> updateAgent(Integer id, Agent agent);

    Flux<Agent> getAllAgents();

    Mono<Void> deleteAgent(Integer id);

    Flux<AgentTool> getAllTools();

    Mono<Void> linkToolToAgent(Integer agentId, String toolName, String routeAgent);

    Mono<Void> unlinkToolFromAgent(Integer agentId, String toolName, String routeAgent);

    Flux<RoutingAgent> getRoutingConfigs();

    Mono<RoutingAgent> addRoutingAgent(RoutingAgent routingAgent);

    Mono<RoutingAgent> updateRoutingAgent(RoutingAgent routing);

    Mono<Void> deleteRouteAgent(Integer id);

    Mono<Void> linkAgentToRouteAgent(Integer routeId, Integer agentId, String routeAgent);

    Mono<Void> unlinkAgentFromRouteAgent(Integer routeId, Integer agentId, String routeAgent);

    Mono<Void> markDangerousTool(String toolName, Boolean dangerous, String routeAgent);
    Mono<Void> linkAgentToRouteByName(String routeName, String agentName);

    Mono<Void> unlinkAgentFromRouteByName(String routeName, String agentName);

    Mono<String> copyTool(AgentTool tool);
    Mono<Void> deleteTool(Integer tool);

    Mono<Void> updateTool(Integer id, AgentTool tool);
}
