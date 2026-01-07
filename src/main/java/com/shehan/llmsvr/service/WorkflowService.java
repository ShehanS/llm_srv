package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.WorkflowDefinition;

public interface WorkflowService {
    WorkflowDefinition load(String workflowId);

}
