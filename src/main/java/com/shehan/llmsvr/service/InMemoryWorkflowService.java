package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.WorkflowDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class InMemoryWorkflowService implements WorkflowService {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {

            // 🔹 Flow 1: WhatsApp → Gmail
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
}
