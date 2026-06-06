package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

@ActivityInterface
public interface WorkflowNode {

    String getType();
    @ActivityMethod
    NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception;
}
