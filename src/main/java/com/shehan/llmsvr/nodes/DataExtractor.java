package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.*;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.LogicEvaluator;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import com.shehan.llmsvr.service.WorkflowEngine;
import com.shehan.llmsvr.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Component
@Slf4j
public class DataExtractor implements WorkflowNode {

    private WorkflowEngine workflowEngine;
    private WorkflowService workflowService;

    @Autowired
    public void setWorkflowEngine(@Lazy WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @Autowired
    public void setWorkflowService(@Lazy WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public String getType() {
        return "data.extractor";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {
        try {
            Map<String, Object> fullContext = Map.of(
                    "all", input,
                    "items", input.getItems()
            );

            Map<String, Object> resolvedData = ExpressionResolver.resolve(config, "mapper", fullContext, new HashMap<>());
            List<Map<String, Object>> logicList = NodeConfigUtil.getInputPropList(config, "logic", Collections.emptyList());
            String flowId = ExpressionResolver.getFlowId(input);

            List<String> targetNodeIds = LogicEvaluator.evaluateAllLogic(logicList, resolvedData);

            if (targetNodeIds.isEmpty()) {
                return NodeResult.skip(new MessageBatch(List.of(new WorkflowMessage(resolvedData))));
            }

            MessageBatch sharedOutputBatch = new MessageBatch(List.of(new WorkflowMessage(resolvedData)));

            workflowService.open(flowId)
                    .flatMap(wf -> workflowEngine.runMultipleNodes(
                            sharedOutputBatch,
                            wf.getDefinition(),
                            targetNodeIds,
                            flowId
                    ))
                    .subscribeOn(Schedulers.parallel())
                    .subscribe(
                            id -> log.info("Successfully triggered multiple nodes for flow: {}", id),
                            error -> log.error("Error triggering multiple nodes for flow {}: {}", flowId, error.getMessage())
                    );

            return NodeResult.complected(targetNodeIds.get(0), sharedOutputBatch);

        } catch (Exception e) {
            log.error("DataExtractor error", e);
            return NodeResult.error(input);
        }
    }
}
