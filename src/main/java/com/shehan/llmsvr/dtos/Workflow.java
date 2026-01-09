package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.WorkflowEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workflow extends BaseClass<Workflow, WorkflowEntity> {
    private Long id;
    private String flowId;
    private String flowName;
    private String description;
    private Definition definition;
    private boolean state;
    private Instant createdAt;
    private Instant updatedAt;
}
