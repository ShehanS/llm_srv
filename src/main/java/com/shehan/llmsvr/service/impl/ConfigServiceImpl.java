package com.shehan.llmsvr.service.impl;


import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingConfig;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.RoutingConfigEntity;
import com.shehan.llmsvr.entites.ToolEntity;
import com.shehan.llmsvr.repositories.AgentRepository;
import com.shehan.llmsvr.repositories.RoutingRepository;
import com.shehan.llmsvr.repositories.ToolRepository;
import com.shehan.llmsvr.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final AgentRepository agentRepository;
    private final ToolRepository toolRepository;
    private final RoutingRepository routingRepository;


    @Override
    public Mono<MainConfig> getFullConfig(String routeName) {
        return Mono.zip(
                getAllAgents().collectList(),
                Mono.fromCallable(() -> routingRepository.findByRouteName(routeName)
                                .orElseThrow(() -> new RuntimeException("Routing config not found for: " + routeName)))
                        .subscribeOn(Schedulers.boundedElastic())
        ).map(tuple -> MainConfig.builder()
                .agents(tuple.getT1())
                .routing(RoutingConfig.builder()
                        .routeName(tuple.getT2().getRouteName())
                        .classifierModel(tuple.getT2().getClassifierModel())
                        .fallbackAgent(tuple.getT2().getFallbackAgent())
                        .routingPrompt(tuple.getT2().getRoutingPrompt())
                        .build())
                .build());
    }

    @Override
    public Mono<RoutingConfig> getRouteConfig(String routeName) {
        return Mono.fromCallable(() -> routingRepository.findByRouteName(routeName))
                .map(entity -> RoutingConfig.fromEntity(entity.get(), RoutingConfig.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Agent> addAgent(AgentEntity agent) {
        return Mono.fromCallable(() -> agentRepository.save(agent))
                .map(entity -> Agent.fromEntity(entity, Agent.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Agent> updateAgent(Integer id, Agent agent) {
        return Mono.fromCallable(() -> {
                    AgentEntity existing = agentRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
                    agent.setId(existing.getId());
                    return agentRepository.save(agent.toEntity(AgentEntity.class));
                }).map(entity -> Agent.fromEntity(entity, Agent.class))
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
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> deleteRouteConfig(Integer id) {
        return Mono.fromRunnable(() -> routingRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }


    @Override
    @Transactional
    public Mono<Tool> addTool(ToolEntity tool) {
        return Mono.fromCallable(() -> toolRepository.save(tool))
                .map(entity -> Tool.fromEntity(entity, Tool.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Tool> updateTool(Integer id, Tool tool) {
        return Mono.fromCallable(() -> {
                    ToolEntity existing = toolRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Tool not found with id: " + id));
                    tool.setId(existing.getId());
                    return toolRepository.save(tool.toEntity(ToolEntity.class));
                }).map(entity -> Tool.fromEntity(entity, Tool.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Tool> getAllTools() {
        return Flux.defer(() -> Flux.fromIterable(toolRepository.findAll()))
                .map(entity -> Tool.fromEntity(entity, Tool.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteTool(Integer id) {
        return Mono.fromRunnable(() -> toolRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }


    @Override
    @Transactional
    public Mono<Void> linkToolToAgent(Integer agentId, Integer toolId) {
        return Mono.fromRunnable(() -> {
            AgentEntity agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            ToolEntity tool = toolRepository.findById(toolId)
                    .orElseThrow(() -> new RuntimeException("Tool not found"));
            agent.getTools().add(tool);
            agentRepository.save(agent);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    @Transactional
    public Mono<Void> unlinkToolFromAgent(Integer agentId, Integer toolId) {
        return Mono.fromRunnable(() -> {
            AgentEntity agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            agent.getTools().removeIf(t -> t.getId().equals(toolId));
            agentRepository.save(agent);
        }).subscribeOn(Schedulers.boundedElastic()).then();
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
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<RoutingConfig> updateRoutingConfig(RoutingConfig routing) {
        return Mono.fromCallable(() -> {
                    RoutingConfigEntity routingConfig = routingRepository.save(routing.toEntity(RoutingConfigEntity.class));
                    return RoutingConfig.fromEntity(routingConfig, RoutingConfig.class);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
