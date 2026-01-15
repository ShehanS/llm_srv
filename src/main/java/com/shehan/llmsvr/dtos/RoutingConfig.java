package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.ModelConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoutingConfig{
    private ModelConfig classifierModel;
    private String fallbackAgent;
    private String routingPrompt;
}
