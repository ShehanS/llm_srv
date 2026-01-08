package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkflowService {
    WorkflowDefinition load(String workflowId);

    Mono<Workflow> save(Workflow workflow);

    Mono<Workflow> open(String id);

    public Flux<Workflow> getAll();
}
