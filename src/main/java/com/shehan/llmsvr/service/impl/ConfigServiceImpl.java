package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingConfig;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.RoutingConfigEntity;
import com.shehan.llmsvr.event.EventPublisher;
import com.shehan.llmsvr.repositories.AgentRepository;
import com.shehan.llmsvr.repositories.RoutingRepository;
import com.shehan.llmsvr.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final AgentRepository agentRepository;
    private final RoutingRepository routingRepository;
    private final EventPublisher eventPublisher;
    private final List<ToolCallback> toolCallbacks;

    @Override
    public Mono<MainConfig> getFullConfig(String routeName) {
        return Mono.fromCallable(() -> routingRepository.findByRouteName(routeName)
                        .orElseThrow(() -> new RuntimeException("Routing config not found for: " + routeName)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(entity -> {
                    List<Agent> assignedAgents = entity.getAgents().stream()
                            .map(agentEntity -> Agent.fromEntity(agentEntity, Agent.class))
                            .toList();
                    return MainConfig.builder()
                            .agents(assignedAgents)
                            .routing(RoutingConfig.builder()
                                    .id(entity.getId())
                                    .routeName(entity.getRouteName())
                                    .classifierModel(entity.getClassifierModel())
                                    .fallbackAgent(entity.getFallbackAgent())
                                    .routingPrompt(entity.getRoutingPrompt())
                                    .build())
                            .build();
                });
    }

    @Override
    public Mono<RoutingConfig> getRouteConfig(String routeName) {
        return Mono.fromCallable(() -> routingRepository.findByRouteName(routeName))
                .flatMap(Mono::justOrEmpty)
                .map(entity -> RoutingConfig.fromEntity(entity, RoutingConfig.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Agent> addAgent(AgentEntity agent) {
        return Mono.fromCallable(() -> agentRepository.save(agent))
                .map(entity -> Agent.fromEntity(entity, Agent.class))
                .doOnSuccess(savedAgent -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Agent> updateAgent(Integer id, Agent agent) {
        return Mono.fromCallable(() -> {
                    AgentEntity existing = agentRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));

                    AgentEntity toSave = agent.toEntity(AgentEntity.class);
                    toSave.setId(existing.getId());
                    return agentRepository.save(toSave);
                }).map(entity -> Agent.fromEntity(entity, Agent.class))
                .doOnSuccess(updateAgent -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Agent> getAllAgents() {
        return Flux.defer(() -> Flux.fromIterable(agentRepository.findAll()))
                .map(entity -> Agent.fromEntity(entity, Agent.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Void> deleteAgent(Integer id) {
        return Mono.fromRunnable(() -> agentRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> deleteRouteConfig(Integer id) {
        return Mono.fromRunnable(() -> routingRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    public Flux<Tool> getAllTools() {
        return Flux.fromIterable(toolCallbacks)
                .map(tool -> {
                    String name = tool.getToolDefinition().name();
                    String description = tool.getToolDefinition().description();
                    return Tool.builder()
                            .name(name)
                            .description(description)
                            .type("mcp")
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Void> linkToolToAgent(Integer agentId, String toolName) {
        return Mono.fromRunnable(() -> {
                    AgentEntity agent = agentRepository.findById(agentId)
                            .orElseThrow(() -> new RuntimeException("Agent not found"));
                    agent.getTools().add(toolName);
                    agentRepository.save(agent);
                }).doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> unlinkToolFromAgent(Integer agentId, String toolName) {
        return Mono.fromRunnable(() -> {
                    AgentEntity agent = agentRepository.findById(agentId)
                            .orElseThrow(() -> new RuntimeException("Agent not found"));
                    agent.getTools().remove(toolName);
                    agentRepository.save(agent);
                }).doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Flux<RoutingConfig> getRoutingConfigs() {
        return Flux.defer(() -> Flux.fromIterable(routingRepository.findAll()))
                .map(entity -> RoutingConfig.fromEntity(entity, RoutingConfig.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<RoutingConfig> addRoutingConfigs(RoutingConfig routing) {
        return Mono.fromCallable(() -> {
                    RoutingConfigEntity routingConfig = routingRepository.save(routing.toEntity(RoutingConfigEntity.class));
                    return RoutingConfig.fromEntity(routingConfig, RoutingConfig.class);
                })
                .doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<RoutingConfig> updateRoutingConfig(RoutingConfig routing) {
        return Mono.fromCallable(() -> {
                    RoutingConfigEntity routingConfig = routingRepository.save(routing.toEntity(RoutingConfigEntity.class));
                    return RoutingConfig.fromEntity(routingConfig, RoutingConfig.class);
                })
                .doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Void> linkAgentToRoute(Integer routeId, Integer agentId) {
        return Mono.zip(
                        Mono.fromCallable(() -> routingRepository.findById(routeId))
                                .flatMap(Mono::justOrEmpty)
                                .switchIfEmpty(Mono.error(new RuntimeException("Route not found"))),
                        Mono.fromCallable(() -> agentRepository.findById(agentId))
                                .flatMap(Mono::justOrEmpty)
                                .switchIfEmpty(Mono.error(new RuntimeException("Agent not found")))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tuple -> {
                    RoutingConfigEntity route = tuple.getT1();
                    AgentEntity agent = tuple.getT2();
                    route.getAgents().add(agent);
                    return Mono.fromCallable(() -> routingRepository.save(route))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                })
                .doOnSuccess(v -> eventPublisher.configSaved());
    }

    @Override
    @Transactional
    public Mono<Void> unlinkAgentFromRoute(Integer routeId, Integer agentId) {
        return Mono.fromCallable(() -> routingRepository.findById(routeId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(Mono::justOrEmpty)
                .switchIfEmpty(Mono.error(new RuntimeException("Route not found")))
                .flatMap(route -> {
                    boolean removed = route.getAgents().removeIf(agent -> agent.getId().equals(agentId));
                    if (removed) {
                        return Mono.fromCallable(() -> routingRepository.save(route))
                                .subscribeOn(Schedulers.boundedElastic())
                                .then();
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> eventPublisher.configSaved());
    }

    @Override
    @Transactional
    public Mono<Void> linkAgentToRouteByName(String routeName, String agentName) {
        return Mono.zip(
                Mono.justOrEmpty(routingRepository.findByRouteName(routeName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Route '" + routeName + "' not found"))),
                Mono.justOrEmpty(agentRepository.findByAgentName(agentName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Agent '" + agentName + "' not found")))
        ).flatMap(tuple -> {
            RoutingConfigEntity route = tuple.getT1();
            AgentEntity agent = tuple.getT2();
            route.getAgents().add(agent);
            routingRepository.save(route);
            return Mono.empty();
        }).doOnSuccess(v -> eventPublisher.configSaved()).then();
    }

    @Override
    @Transactional
    public Mono<Void> unlinkAgentFromRouteByName(String routeName, String agentName) {
        return Mono.zip(
                Mono.justOrEmpty(routingRepository.findByRouteName(routeName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Route '" + routeName + "' not found"))),
                Mono.justOrEmpty(agentRepository.findByAgentName(agentName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Agent '" + agentName + "' not found")))
        ).flatMap(tuple -> {
            RoutingConfigEntity route = tuple.getT1();
            AgentEntity agent = tuple.getT2();
            if (route.getAgents().remove(agent)) {
                routingRepository.save(route);
            }
            return Mono.empty();
        }).doOnSuccess(v -> eventPublisher.configSaved()).then();
    }
}
