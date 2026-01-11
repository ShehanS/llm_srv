package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import com.shehan.llmsvr.dtos.WorkflowMessage;
import com.shehan.llmsvr.helper.ExpressionResolver;
import com.shehan.llmsvr.helper.NodeConfigUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpResponseNode implements WorkflowNode {

    @Override
    public String getType() {
        return "http.response";
    }
    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) {

        WorkflowMessage msg = input.getItems().get(0);
        Map<String, Object> ctx = msg.getData();

        Object bodyExpr =
                NodeConfigUtil.getInputProp(config, "body", "{{body}}");

        Object resolved =
                ExpressionResolver.resolve(
                        String.valueOf(bodyExpr),
                        ctx
                );

        ctx.put("__httpResponse__", resolved);
        return new NodeResult("stop", input);
    }
}
