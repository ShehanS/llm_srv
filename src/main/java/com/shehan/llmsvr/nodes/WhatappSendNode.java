package com.shehan.llmsvr.nodes;

import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class WhatappSendNode implements WorkflowNode {
    @Override
    public String getType() {
        return "whatsapp.send";
    }

    @Override
    public NodeResult execute(MessageBatch input, Map<String, Object> config) throws Exception {
        return null;
    }
}
