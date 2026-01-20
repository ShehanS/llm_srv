package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TraceNode implements WorkflowNode {
    @Override
    public String getType() {
        return "trace.inbound";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception {
        log.info(input.toString());
        return NodeResult.complected("success", new MessageBatch(List.of()));
    }

}
