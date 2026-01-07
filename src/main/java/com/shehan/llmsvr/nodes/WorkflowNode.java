package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;

import java.util.Map;

public interface WorkflowNode {

    String getType();

    NodeResult execute(MessageBatch input, Map<String, Object> config);
}
