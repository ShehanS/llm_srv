package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.ModelConfig;
import com.shehan.llmsvr.entites.RoutingConfigEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RoutingConfig extends BaseClass<RoutingConfig, RoutingConfigEntity> {
    private Integer id;
    private String routeName;
    private ModelConfig classifierModel;
    private String fallbackAgent;
    private String routingPrompt;
    private List<Agent> agents;
}
