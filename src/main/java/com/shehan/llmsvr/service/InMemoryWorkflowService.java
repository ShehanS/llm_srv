package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Workflow;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import com.shehan.llmsvr.entites.WorkflowEntity;
import com.shehan.llmsvr.repositories.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            workflows.put("wf-0", mapper.readValue("""
                    {"nodes":[{"id":"2274f12e-946c-4316-97cd-9f692b2d0c8c","type":"trigger.http","label":"HTTP Webhook","color":"#b21531","position":{"x":-174.0625,"y":292},"config":{"icon":"webhook","inputProps":[{"type":"select","name":"method","displayName":"HTTP Method","values":[{"name":"POST","value":"POST"},{"name":"GET","value":"GET"}],"defaultValue":"POST","required":true,"value":null},{"type":"text","name":"path","displayName":"Endpoint Path","defaultValue":"/webhook","required":true,"value":"/test"}]},"inputs":[],"outputs":[{"id":"default","label":"Default","position":"right"}]},{"id":"57e44b64-a4f0-4489-ab41-8a4b36045b20","type":"trigger.whatsapp","label":"WhatsApp Trigger","color":"#6366f1","position":{"x":27.4375,"y":393.5},"config":{"icon":"whatsapp","inputProps":[{"type":"text","name":"verifyToken","displayName":"Verify Token","defaultValue":"","value":"test","required":true,"placeholder":"Meta webhook verify token"},{"type":"text","name":"authHeader","displayName":"Auth Header Name","defaultValue":"X-API-KEY","value":"X-API-KEY","required":true,"placeholder":"Header used for auth"}]},"inputs":[{"id":"default","label":"HTTP Payload","position":"left"}],"outputs":[{"id":"default","label":"Verified Message","position":"right"}]}],"edges":[{"source":"2274f12e-946c-4316-97cd-9f692b2d0c8c","target":"57e44b64-a4f0-4489-ab41-8a4b36045b20","sourceHandle":"default"}]}
                    """, WorkflowDefinition.class));

            workflows.put("wf-1", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.whatsapp", "config": {} },
                        { "id": "2", "type": "whatsapp.send", "config": { "template": "received" } },
                        { "id": "3", "type": "gmail.send", "config": { "to": "admin@test.com" } }
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" },
                        { "source": "2", "target": "3", "sourceHandle": "success" }
                      ]
                    }
                    """, WorkflowDefinition.class));
        // 🔹 Flow 6: WhatsApp → Gmail + WhatsApp (PARALLEL)
            workflows.put("wf-6", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.whatsapp", "config": { "apiKey": "test" } },

                        { "id": "2", "type": "gmail.send", "config": {
                          "to": "admin@test.com",
                          "subject": "WhatsApp Received"
                        }},

                        { "id": "3", "type": "whatsapp.send", "config": {
                          "template": "thanks_for_message"
                        }}
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" },
                        { "source": "1", "target": "3", "sourceHandle": "default" }
                      ]
                    }
                    """, WorkflowDefinition.class));

            // 🔹 Flow 2: WhatsApp → Conditional → Gmail OR WhatsApp
            workflows.put("wf-2", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.whatsapp", "config": {} },
                        { "id": "2", "type": "condition.text", "config": { "contains": "help" } },
                        { "id": "3", "type": "gmail.send", "config": { "to": "support@test.com" } },
                        { "id": "4", "type": "whatsapp.send", "config": { "template": "auto_reply" } }
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" },
                        { "source": "2", "target": "3", "sourceHandle": "true" },
                        { "source": "2", "target": "4", "sourceHandle": "false" }
                      ]
                    }
                    """, WorkflowDefinition.class));

            // 🔹 Flow 3: Webhook → WhatsApp → WhatsApp (Follow-up)
            workflows.put("wf-3", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.webhook", "config": {} },
                        { "id": "2", "type": "whatsapp.send", "config": { "template": "order_received" } },
                        { "id": "3", "type": "whatsapp.send", "config": { "template": "order_followup" } }
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" },
                        { "source": "2", "target": "3", "sourceHandle": "success" }
                      ]
                    }
                    """, WorkflowDefinition.class));

            // 🔹 Flow 4: Webhook → Gmail only
            workflows.put("wf-4", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.webhook", "config": {} },
                        { "id": "2", "type": "gmail.send", "config": {
                          "to": "ops@test.com",
                          "subject": "New Webhook Event"
                        }}
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" }
                      ]
                    }
                    """, WorkflowDefinition.class));

            // 🔹 Flow 5: WhatsApp → Gmail (success) | Gmail (error)
            workflows.put("wf-5", mapper.readValue("""
                    {
                      "nodes": [
                        { "id": "1", "type": "trigger.whatsapp", "config": {} },
                        { "id": "2", "type": "whatsapp.send", "config": { "template": "notify" } },
                        { "id": "3", "type": "gmail.send", "config": { "to": "success@test.com" } },
                        { "id": "4", "type": "gmail.send", "config": { "to": "error@test.com" } }
                      ],
                      "edges": [
                        { "source": "1", "target": "2", "sourceHandle": "default" },
                        { "source": "2", "target": "3", "sourceHandle": "success" },
                        { "source": "2", "target": "4", "sourceHandle": "error" }
                      ]
                    }
                    """, WorkflowDefinition.class));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public WorkflowDefinition load(String workflowId) {
        return workflows.get(workflowId);
    }

    @Override
    @Transactional
    public Mono<Workflow> save(Workflow workflow) {
        return Mono.fromCallable(() -> {
            Optional<WorkflowEntity> existing =
                    workflowRepository.findFlow(workflow.getFlowId(), workflow.getFlowName());
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

        }).subscribeOn(Schedulers.boundedElastic());
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
