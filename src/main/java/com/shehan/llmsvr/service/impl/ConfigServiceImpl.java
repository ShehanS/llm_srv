package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.AgentTool;
import com.shehan.llmsvr.dtos.MainConfig;
import com.shehan.llmsvr.dtos.RoutingAgent;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.AgentToolEntity;
import com.shehan.llmsvr.entites.DangerousToolEntity;
import com.shehan.llmsvr.entites.RoutingAgentEntity;
import com.shehan.llmsvr.event.EventPublisher;
import com.shehan.llmsvr.repositories.AgentRepository;
import com.shehan.llmsvr.repositories.AgentToolRepository;
import com.shehan.llmsvr.repositories.DangerousToolRepository;
import com.shehan.llmsvr.repositories.RoutingAgentRepository;
import com.shehan.llmsvr.service.CommonToolService;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final AgentRepository agentRepository;
    private final RoutingAgentRepository routingAgentRepository;
    private final EventPublisher eventPublisher;
    private final List<ToolCallback> toolCallbacks;
    private final CommonToolService commonToolService;
    private final AgentToolRepository agentToolRepository;


    private final DangerousToolRepository dangerousToolRepository;

    @Override
    public Mono<MainConfig> getFullConfig(String routeName) {
        return Mono.fromCallable(() -> routingAgentRepository.findByRouteName(routeName)
                        .orElseThrow(() -> new RuntimeException("Routing config not found for: " + routeName)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> {
                    return Mono.fromCallable(() -> dangerousToolRepository.findAll().stream()
                                    .filter(DangerousToolEntity::getDangerous)
                                    .map(DangerousToolEntity::getToolName)
                                    .collect(Collectors.toSet()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(dangerousNames -> {
                                List<Agent> assignedAgents = entity.getAgents().stream()
                                        .map(agentEntity -> Agent.fromEntity(agentEntity, Agent.class))
                                        .toList();

                                return MainConfig.builder()
                                        .agents(assignedAgents)
                                        .dangerousTools(dangerousNames)
                                        .routing(RoutingAgent.builder()
                                                .id(entity.getId())
                                                .routeName(entity.getRouteName())
                                                .classifierModel(entity.getClassifierModel())
                                                .fallbackAgent(entity.getFallbackAgent())
                                                .routingPrompt(entity.getRoutingPrompt())
                                                .build())
                                        .build();
                            });
                });
    }
    @Override
    public Mono<RoutingAgent> getRouteConfig(String routeName) {
        return Mono.fromCallable(() -> routingAgentRepository.findByRouteName(routeName))
                .flatMap(Mono::justOrEmpty)
                .map(entity -> RoutingAgent.fromEntity(entity, RoutingAgent.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Agent> addAgent(Agent agent) {
        return Mono.fromCallable(() -> {
                    AgentEntity entity = agent.toEntity(AgentEntity.class);
                    if (agent.getId() != null) {
                        entity.setId(agent.getId());
                    }
                    AgentEntity savedEntity = agentRepository.save(entity);
                    return Agent.fromEntity(savedEntity, Agent.class);
                })
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
    @Transactional(readOnly = true)
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
    public Mono<Void> deleteRouteAgent(Integer id) {
        return Mono.fromRunnable(() -> routingAgentRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    public Flux<AgentTool> getAllTools() {
        return Flux.fromIterable(agentToolRepository.findAll()
                .stream()
                .map(t -> AgentTool.fromEntity(t, AgentTool.class)).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Mono<Void> linkToolToAgent(Integer agentId, String toolName) {
        return Mono.fromRunnable(() -> {
                    AgentEntity agent = agentRepository.findById(agentId)
                            .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
                    AgentToolEntity tool = agentToolRepository.getToolByName(toolName)
                            .orElseThrow(() -> new RuntimeException("Tool not found with name: " + toolName));
                    agent.getTools().add(tool);
                    agentRepository.save(agent);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> unlinkToolFromAgent(Integer agentId, String toolName) {
        return Mono.fromRunnable(() -> {
                    AgentEntity agent = agentRepository.findById(agentId)
                            .orElseThrow(() -> new RuntimeException("Agent not found"));
                    AgentToolEntity toolToRemove = agent.getTools().stream()
                            .filter(t -> t.getToolName().equals(toolName))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Tool association not found"));
                    agent.getTools().remove(toolToRemove);
                    agentRepository.save(agent);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    public Flux<RoutingAgent> getRoutingConfigs() {
        return Flux.defer(() -> Flux.fromIterable(routingAgentRepository.findAll()))
                .map(entity -> RoutingAgent.fromEntity(entity, RoutingAgent.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<RoutingAgent> addRoutingAgent(RoutingAgent routing) {
        return Mono.fromCallable(() -> {
                    RoutingAgentEntity entity = routing.toEntity(RoutingAgentEntity.class);
                    if (routing.getId() != null) {
                        entity.setId(routing.getId());
                    }
                    RoutingAgentEntity savedEntity = routingAgentRepository.save(entity);
                    return RoutingAgent.fromEntity(savedEntity, RoutingAgent.class);
                })
                .doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }
    @Override
    @Transactional
    public Mono<RoutingAgent> updateRoutingAgent(RoutingAgent routing) {
        return Mono.fromCallable(() -> {
                    RoutingAgentEntity routingConfig = routingAgentRepository.save(routing.toEntity(RoutingAgentEntity.class));
                    return RoutingAgent.fromEntity(routingConfig, RoutingAgent.class);
                })
                .doOnSuccess(v -> eventPublisher.configSaved())
                .subscribeOn(Schedulers.boundedElastic());
    }


    @Override
    @Transactional
    public Mono<Void> linkAgentToRouteAgent(Integer routeId, Integer agentId) {
        return Mono.fromRunnable(() -> {
                    RoutingAgentEntity route = routingAgentRepository.findById(routeId)
                            .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));

                    AgentEntity agent = agentRepository.findById(agentId)
                            .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));

                    route.getAgents().add(agent);
                    routingAgentRepository.save(route);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }
    @Override
    @Transactional
    public Mono<Void> unlinkAgentFromRouteAgent(Integer routeId, Integer agentId) {
        return Mono.fromRunnable(() -> {
                    RoutingAgentEntity route = routingAgentRepository.findById(routeId)
                            .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
                    boolean removed = route.getAgents().removeIf(agent -> agent.getId().equals(agentId));

                    if (removed) {
                        routingAgentRepository.save(route);
                    } else {
                        throw new RuntimeException("Agent association not found in this route");
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }
    @Override
    @Transactional
    public Mono<Void> markDangerousTool(String toolName, Boolean dangerous) {
        return Mono.fromCallable(() -> {
                    DangerousToolEntity toolEntity = dangerousToolRepository.findByToolName(toolName)
                            .orElse(DangerousToolEntity.builder()
                                    .toolName(toolName)
                                    .build());


                    toolEntity.setDangerous(dangerous);
                    return dangerousToolRepository.save(toolEntity);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> eventPublisher.configSaved())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> linkAgentToRouteByName(String routeName, String agentName) {
        return Mono.zip(
                Mono.justOrEmpty(routingAgentRepository.findByRouteName(routeName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Route '" + routeName + "' not found"))),
                Mono.justOrEmpty(agentRepository.findByAgentName(agentName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Agent '" + agentName + "' not found")))
        ).flatMap(tuple -> {
            RoutingAgentEntity route = tuple.getT1();
            AgentEntity agent = tuple.getT2();
            route.getAgents().add(agent);
            routingAgentRepository.save(route);
            return Mono.empty();
        }).doOnSuccess(v -> eventPublisher.configSaved()).then();
    }

    @Override
    @Transactional
    public Mono<Void> unlinkAgentFromRouteByName(String routeName, String agentName) {
        return Mono.zip(
                Mono.justOrEmpty(routingAgentRepository.findByRouteName(routeName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Route '" + routeName + "' not found"))),
                Mono.justOrEmpty(agentRepository.findByAgentName(agentName))
                        .switchIfEmpty(Mono.error(new RuntimeException("Agent '" + agentName + "' not found")))
        ).flatMap(tuple -> {
            RoutingAgentEntity route = tuple.getT1();
            AgentEntity agent = tuple.getT2();
            if (route.getAgents().remove(agent)) {
                routingAgentRepository.save(route);
            }
            return Mono.empty();
        }).doOnSuccess(v -> eventPublisher.configSaved()).then();
    }

    @Override
    public Mono<String> copyTool(AgentTool tool) {
        return commonToolService.loadCommonTool(tool.getCopyFrom())
                .flatMap(commonTool -> Mono.fromCallable(() -> {
                    String finalName;
                    String originalName = commonTool.getToolName();
                    String requestedName = tool.getToolName();

                    boolean nameExists = (requestedName != null && !requestedName.isEmpty())
                            && agentToolRepository.existsByToolName(requestedName);

                    if (requestedName == null || requestedName.isEmpty() || requestedName.equals(originalName) || nameExists) {
                        String baseName = originalName + "_copy";
                        finalName = baseName;
                        int counter = 1;
                        while (agentToolRepository.existsByToolName(finalName)) {
                            finalName = baseName + "_" + counter;
                            counter++;
                        }
                    } else {
                        finalName = requestedName;
                    }

                    AgentToolEntity newEntity = AgentToolEntity.builder()
                            .toolName(finalName)
                            .toolDisplayName(tool.getToolDisplayName())
                            .descriptions(tool.getDescription())
                            .type(commonTool.getType())
                            .source(commonTool.getSource())
                            .dangerous(tool.getDangerous())
                            .copyFrom(commonTool.getToolName())
                            .build();

                    AgentToolEntity saved = agentToolRepository.save(newEntity);
                    return "Tool copied successfully as: " + saved.getToolName();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.error("Failed to copy tool: {}", ex.getMessage());
                    return Mono.error(new RuntimeException("Error: Could not copy tool. " + ex.getMessage()));
                });
    }

    @Override
    public Mono<Void> deleteTool(Integer toolId) {
        return Mono.fromCallable(() -> {
                    agentToolRepository.deleteById(toolId);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> updateTool(Integer id, AgentTool tool) {
        return Mono.fromCallable(() -> {
                    AgentToolEntity existingEntity = agentToolRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Tool not found with id: " + id));
                    existingEntity.setSource(tool.getSource());
                    existingEntity.setDangerous(tool.getDangerous());
                    if (tool.getToolName() != null) {
                        existingEntity.setToolName(tool.getToolName());
                    }
                    agentToolRepository.save(existingEntity);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
