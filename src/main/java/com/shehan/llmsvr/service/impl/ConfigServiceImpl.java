package com.shehan.llmsvr.service.impl;


import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingConfig;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.RoutingEntity;
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
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final AgentRepository agentRepository;
    private final ToolRepository toolRepository;
    private final RoutingRepository routingRepository;


    @Override
    public Mono<MainConfig> getFullConfig() {
        return Mono.zip(
                getAllAgents().collectList(),
                getRoutingConfig()
        ).map(tuple -> MainConfig.builder()
                .agents(tuple.getT1())
                .routing(RoutingConfig.builder()
                        .classifierModel(tuple.getT2().getClassifierModel())
                        .fallbackAgent(tuple.getT2().getFallbackAgent())
                        .routingPrompt(tuple.getT2().getRoutingPrompt())
                        .build())
                .build());
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
    public Mono<Agent> updateAgent(Integer id, AgentEntity agent) {
        return Mono.fromCallable(() -> {
                    AgentEntity existing = agentRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
                    agent.setId(existing.getId());
                    return agentRepository.save(agent);
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
    public Mono<Tool> addTool(ToolEntity tool) {
        return Mono.fromCallable(() -> toolRepository.save(tool))
                .map(entity -> Tool.fromEntity(entity, Tool.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Tool> updateTool(Integer id, ToolEntity tool) {
        return Mono.fromCallable(() -> {
                    ToolEntity existing = toolRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Tool not found with id: " + id));
                    tool.setId(existing.getId());
                    return toolRepository.save(tool);
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
    public Mono<RoutingEntity> getRoutingConfig() {
        return Mono.fromCallable(() -> routingRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Routing config missing")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<RoutingEntity> updateRoutingConfig(RoutingEntity routing) {
        return Mono.fromCallable(() -> routingRepository.save(routing))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
