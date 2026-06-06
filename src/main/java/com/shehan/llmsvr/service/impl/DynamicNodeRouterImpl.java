package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.nodes.WorkflowNode;
import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DynamicNodeRouterImpl implements DynamicActivity {
    private final List<WorkflowNode> nodes;

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(EncodedValues args) {
        String requestedActivityType = Activity.getExecutionContext().getInfo().getActivityType();
        log.info("Intercepted task request for dynamic routing type: {}", requestedActivityType);
        MessageBatch inputBatch = args.get(0, MessageBatch.class);
        Map<String, Object> config = args.get(1, Map.class);
        for (WorkflowNode node : nodes) {
            if (requestedActivityType.equals(node.getType())) {
                try {
                    return node.execute(inputBatch, config);
                } catch (Exception e) {
                    throw Activity.wrap(e);
                }
            }
        }

        throw new IllegalArgumentException("No matching structural execution node bean found for activity type: " + requestedActivityType);
    }
}
