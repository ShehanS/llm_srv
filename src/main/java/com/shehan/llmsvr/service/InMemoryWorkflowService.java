package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.FlowNode;
import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import com.shehan.llmsvr.entites.WorkflowEntity;
import com.shehan.llmsvr.event.EventPublisher;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import com.shehan.llmsvr.repositories.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InMemoryWorkflowService implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();

    private final EventPublisher eventPublisher;


    @Override
    public WorkflowDefinition load(String workflowId) {
        return workflows.get(workflowId);
    }

    @Override
    @Transactional
    public Mono<Workflow> save(Workflow workflow) {

        return Mono.fromCallable(() -> {

                    Optional<WorkflowEntity> existing =
                            workflowRepository.findFlow(
                                    workflow.getFlowId(),
                                    workflow.getFlowName()
                            );
                    WorkflowEntity entity;
                    if (existing.isPresent()) {
                        entity = existing.get();
                        entity.setDescription(workflow.getDescription());
                        entity.setDefinition(workflow.getDefinition());
                        entity.setUpdatedAt(Instant.now());
                    } else {
                        entity = workflow.toEntity(WorkflowEntity.class);
                        entity.setCreatedAt(Instant.now());
                    }
                    WorkflowEntity saved = workflowRepository.save(entity);
                    return Workflow.fromEntity(saved, Workflow.class);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(this::notifySave);
    }


    private Mono<Workflow> notifySave(Workflow workflow) {
        Optional<FlowNode> aiAgentTrigger = workflow.getDefinition()
                .getNodes()
                .stream()
                .filter(node -> "trigger.aiAgent".equals(node.getType()))
                .findFirst();
        if (aiAgentTrigger.isPresent()) {
            String routAgentName = NodeConfigUtil.getInputProp(aiAgentTrigger.get().getConfig(), "routeAgent", "");
          log.info("Save and send to notify to load agents {}", routAgentName);
          eventPublisher.configSaved(routAgentName);
        }

        return Mono.just(workflow);
    }


    @Override
    public Mono<Workflow> open(String flowId) {
        return Mono.fromCallable(() ->
                workflowRepository.findByFlowId(flowId)
                        .map(entity -> Workflow.fromEntity(entity, Workflow.class))
                        .orElseThrow(() ->
                                new IllegalArgumentException("Workflow not found: " + flowId)
                        )
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Workflow> getAll() {
        return Flux.fromIterable(workflowRepository.findAll())
                .map(entity -> Workflow.fromEntity(entity, Workflow.class))
                .subscribeOn(Schedulers.boundedElastic());
    }


}
